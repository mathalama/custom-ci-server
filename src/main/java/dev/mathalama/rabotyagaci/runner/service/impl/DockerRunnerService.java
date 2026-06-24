package dev.mathalama.rabotyagaci.runner.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.async.ResultCallback;
import dev.mathalama.rabotyagaci.build.api.event.BuildStepStartedEvent;
import dev.mathalama.rabotyagaci.build.domain.StepStatus;
import dev.mathalama.rabotyagaci.runner.api.event.BuildStepCompletedEvent;
import dev.mathalama.rabotyagaci.runner.api.event.LogChunkEvent;
import dev.mathalama.rabotyagaci.runner.config.RunnerConfig;
import dev.mathalama.rabotyagaci.runner.exception.DockerExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerRunnerService {

    private final DockerClient dockerClient;
    private final RunnerConfig runnerConfig;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    public void handleBuildStepStarted(BuildStepStartedEvent event) {
        log.info("Received BuildStepStartedEvent for build ID: {}, step ID: {}, step name: {}",
                event.buildId(), event.stepId(), event.stepName());

        String containerId = null;
        ResultCallback.Adapter<Frame> logCallback = null;
        int exitCode = -1;
        StepStatus status = StepStatus.FAILURE;

        try {
            // 1. Ensure workspace directory exists
            Path workspacePath = event.workspaceDir();
            if (!Files.exists(workspacePath)) {
                Files.createDirectories(workspacePath);
            }

            // 2. Pull the required image
            pullImageIfNeeded(event.dockerImage());

            // 3. Configure mount
            // Use the Docker Compose-prefixed volume name (e.g. "rabotyagaci_app-data")
            Volume containerWorkspaceVolume = new Volume("/app-data");
            Bind workspaceBind = new Bind(runnerConfig.getVolumeName(), containerWorkspaceVolume);

            // 4. Configure host limits
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(workspaceBind)
                    .withMemory(parseMemoryLimit(runnerConfig.getContainerMemoryLimit()))
                    .withNanoCPUs(parseCpuLimit(runnerConfig.getContainerCpuLimit()))
                    .withNetworkMode(runnerConfig.getNetworkMode());

            // 5. Build commands shell script
            // Map the backend's internal path (/tmp/rabotyagaci/...) to the container's mounted volume path (/app-data/...)
            String containerWorkingDir = event.workspaceDir().toString().replace("/tmp/rabotyagaci", "/app-data").replace("\\", "/");
            String commandScript = "set -e\ncd " + containerWorkingDir + "\n" + String.join("\n", event.commands());

            log.info("Creating container for step {} with image {}. Volume: {}, workingDir: {}",
                    event.stepName(), event.dockerImage(), runnerConfig.getVolumeName(), containerWorkingDir);

            // Map env variables for container
            java.util.List<String> envList = new java.util.ArrayList<>();
            if (event.environmentVariables() != null) {
                event.environmentVariables().forEach((k, v) -> envList.add(k + "=" + v));
            }

            // 6. Create container
            CreateContainerResponse containerResponse = dockerClient.createContainerCmd(event.dockerImage())
                    .withHostConfig(hostConfig)
                    .withCmd("/bin/sh", "-c", commandScript)
                    .withWorkingDir(containerWorkingDir)
                    .withEnv(envList)
                    .exec();

            containerId = containerResponse.getId();
            log.info("Container created with ID: {}", containerId);

            // 7. Start container
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Container {} started successfully", containerId);

            // 8. Attach log streamer with tailAll to get logs from the very beginning
            AtomicInteger lineNumber = new AtomicInteger(1);
            logCallback = dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTailAll()
                    .exec(new ResultCallback.Adapter<>() {
                        @Override
                        public void onNext(Frame frame) {
                            try {
                                String payload = new String(frame.getPayload(), StandardCharsets.UTF_8);
                                try (BufferedReader reader = new BufferedReader(new StringReader(payload))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        String streamType = frame.getStreamType().name();
                                        eventPublisher.publishEvent(new LogChunkEvent(
                                                event.stepId(),
                                                streamType,
                                                line,
                                                lineNumber.getAndIncrement(),
                                                Instant.now()
                                        ));
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Error formatting/publishing container log frame", e);
                            }
                        }
                    });

            // 9. Wait for completion with timeout
            WaitContainerResultCallback waitCallback = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback());

            long timeoutSeconds = runnerConfig.getDefaultTimeout();
            Integer waitExitCode = waitCallback.awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);

            if (waitExitCode != null) {
                exitCode = waitExitCode;
                status = (exitCode == 0) ? StepStatus.SUCCESS : StepStatus.FAILURE;
                log.info("Container {} finished with exit code: {}", containerId, exitCode);
            } else {
                log.warn("Container {} execution timed out after {} seconds", containerId, timeoutSeconds);
                status = StepStatus.FAILURE;
                exitCode = 137; // SIGKILL equivalent or standard timeout code
            }

        } catch (Exception e) {
            log.error("Fatal error executing build step: {}", event.stepName(), e);
            if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                exitCode = 130; // Standard exit code for termination by Ctrl+C
            } else {
                exitCode = -1;
            }
            status = StepStatus.FAILURE;
        } finally {
            // Close log streaming adapters
            if (logCallback != null) {
                try {
                    logCallback.awaitCompletion(3, TimeUnit.SECONDS);
                    logCallback.close();
                } catch (Exception e) {
                    log.error("Failed to close container log callback", e);
                }
            }

            // Cleanup container
            if (containerId != null) {
                try {
                    log.info("Stopping container: {}", containerId);
                    dockerClient.stopContainerCmd(containerId).exec();
                } catch (Exception e) {
                    log.debug("Container already stopped or failed to stop: {}", e.getMessage());
                }

                try {
                    log.info("Removing container: {}", containerId);
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    log.error("Failed to remove container: {}", containerId, e);
                }
            }

            // Publish BuildStepCompletedEvent
            log.info("Publishing BuildStepCompletedEvent for step ID: {}, status: {}, exitCode: {}",
                    event.stepId(), status, exitCode);
            eventPublisher.publishEvent(new BuildStepCompletedEvent(
                    event.buildId(),
                    event.stepId(),
                    status,
                    exitCode,
                    Instant.now()
            ));
        }
    }

    private void pullImageIfNeeded(String image) {
        try {
            log.info("Pulling Docker image: {}", image);
            dockerClient.pullImageCmd(image)
                    .start()
                    .awaitCompletion();
            log.info("Successfully pulled image: {}", image);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DockerExecutionException("Docker image pull interrupted for image: " + image, e);
        } catch (Exception e) {
            log.warn("Failed to pull image {}. Checking if available locally.", image, e);
        }
    }

    private Long parseMemoryLimit(String memoryLimit) {
        if (memoryLimit == null || memoryLimit.isBlank()) {
            return null;
        }
        String valueStr = memoryLimit.toLowerCase().trim();
        long multiplier = 1;
        if (valueStr.endsWith("k")) {
            multiplier = 1024L;
            valueStr = valueStr.substring(0, valueStr.length() - 1);
        } else if (valueStr.endsWith("m")) {
            multiplier = 1024L * 1024;
            valueStr = valueStr.substring(0, valueStr.length() - 1);
        } else if (valueStr.endsWith("g")) {
            multiplier = 1024L * 1024 * 1024;
            valueStr = valueStr.substring(0, valueStr.length() - 1);
        }
        try {
            return Long.parseLong(valueStr) * multiplier;
        } catch (NumberFormatException e) {
            log.warn("Invalid memory limit configuration: {}. Memory limits will not be set.", memoryLimit);
            return null;
        }
    }

    private Long parseCpuLimit(double cpuLimit) {
        if (cpuLimit <= 0) {
            return null;
        }
        return (long) (cpuLimit * 1_000_000_000L); // HostConfig.withNanoCPUs expects cpu count * 1e9
    }
}
