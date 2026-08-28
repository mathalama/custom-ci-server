package dev.mathalama.rabotyagaci.runner.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.async.ResultCallback;
import dev.mathalama.rabotyagaci.build.api.event.BuildCancelledEvent;
import dev.mathalama.rabotyagaci.build.api.event.BuildStepStartedEvent;
import dev.mathalama.rabotyagaci.build.domain.StepStatus;
import dev.mathalama.rabotyagaci.log.service.LogSanitizer;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerRunnerService {

    private final DockerClient dockerClient;
    private final RunnerConfig runnerConfig;
    private final ApplicationEventPublisher eventPublisher;
    private final dev.mathalama.rabotyagaci.runner.repository.RunnerRepository runnerRepository;
    private final dev.mathalama.rabotyagaci.runner.websocket.RunnerWebSocketHandler runnerWebSocketHandler;
    private final RunnerJobQueueService runnerJobQueueService;
    private final dev.mathalama.rabotyagaci.build.repository.BuildRepository buildRepository;
    private final dev.mathalama.rabotyagaci.build.repository.BuildStepRepository buildStepRepository;
    private final dev.mathalama.rabotyagaci.project.service.impl.SecretCryptoService secretCryptoService;
    private final LogSanitizer logSanitizer;

    private final java.util.concurrent.ConcurrentHashMap<Long, String> activeContainers = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.concurrent.Semaphore buildSemaphore;

    @jakarta.annotation.PostConstruct
    public void init() {
        buildSemaphore = new java.util.concurrent.Semaphore(runnerConfig.getMaxConcurrentBuilds());
    }

    @EventListener
    public void handleBuildCancelled(BuildCancelledEvent event) {
        String containerId = activeContainers.get(event.buildId());
        if (containerId != null) {
            log.info("Received cancellation for build ID {}, killing container {}", event.buildId(), containerId);
            try {
                dockerClient.killContainerCmd(containerId).exec();
            } catch (Exception e) {
                log.warn("Failed to kill container {} for cancelled build {}", containerId, event.buildId(), e);
            }
        }
    }

    @Async
    @EventListener
    public void handleBuildStepStarted(BuildStepStartedEvent event) {
        log.info("Received request to start step: {} for build ID: {}", event.stepName(), event.buildId());

        // Try to find an available online remote runner
        java.util.Optional<dev.mathalama.rabotyagaci.runner.domain.Runner> runnerOpt = runnerRepository.findAll().stream()
                .filter(r -> r.getStatus() == dev.mathalama.rabotyagaci.runner.domain.RunnerStatus.ONLINE)
                .findFirst();

        if (runnerOpt.isPresent()) {
            dev.mathalama.rabotyagaci.runner.domain.Runner runner = runnerOpt.get();
            log.info("Found online remote runner: '{}' (ID: {}). Dispatching step '{}'",
                    runner.getName(), runner.getId(), event.stepName());

            // Mark step as running on this runner in DB
            dev.mathalama.rabotyagaci.build.domain.BuildStep step = buildStepRepository.findById(event.stepId()).orElse(null);
            if (step != null) {
                step.setRunner(runner);
                buildStepRepository.save(step);
            }

            try {
                dev.mathalama.rabotyagaci.runner.api.dto.RunnerJobPayload jobPayload = new dev.mathalama.rabotyagaci.runner.api.dto.RunnerJobPayload(
                        event.buildId(),
                        event.stepId(),
                        event.stepName(),
                        event.dockerImage(),
                        event.commands(),
                        event.environmentVariables(),
                        event.privileged(),
                        event.dockerSocket(),
                        event.secretFiles()
                );

                runnerJobQueueService.enqueueJob(runner.getId(), jobPayload);
                return; // Remote delegation succeeded via pull queue!
            } catch (Exception e) {
                log.error("Failed to delegate task to remote runner pull queue. Falling back to local execution.", e);
            }
        }

        try {
            buildSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted while waiting for build slot for build {}", event.buildId());
            eventPublisher.publishEvent(new BuildStepCompletedEvent(event.buildId(), event.stepId(), StepStatus.FAILURE, -1, Instant.now()));
            return;
        }

        String containerId = null;
        ResultCallback.Adapter<Frame> logCallback = null;
        int exitCode = -1;
        StepStatus status = StepStatus.FAILURE;

        try {
            // Register secrets for log sanitization
            List<String> secretsToMask = new java.util.ArrayList<>();
            if (event.environmentVariables() != null) {
                secretsToMask.addAll(event.environmentVariables().values());
            }
            if (event.secretFiles() != null) {
                secretsToMask.addAll(event.secretFiles().values());
            }
            logSanitizer.registerSession(event.stepId(), secretsToMask);

            // 1. Ensure workspace directory exists
            Path workspacePath = event.workspaceDir();
            if (!Files.exists(workspacePath)) {
                Files.createDirectories(workspacePath);
            }

            // Write secret files to workspace if configured
            if (event.secretFiles() != null && !event.secretFiles().isEmpty()) {
                log.info("Writing {} secret files for step {}", event.secretFiles().size(), event.stepName());
                for (java.util.Map.Entry<String, String> entry : event.secretFiles().entrySet()) {
                    String relativePath = entry.getKey();
                    String content = entry.getValue();
                    
                    Path targetFilePath = workspacePath.resolve(relativePath).normalize();
                    // Prevent path traversal attack
                    if (!targetFilePath.startsWith(workspacePath)) {
                        throw new DockerExecutionException("Invalid secret file path: " + relativePath);
                    }
                    
                    Files.createDirectories(targetFilePath.getParent());
                    Files.writeString(targetFilePath, content);
                    
                    // Set file permissions to 600 for safety (e.g. for SSH keys)
                    if (java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                        try {
                            Files.setPosixFilePermissions(
                                    targetFilePath,
                                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
                            );
                        } catch (Exception e) {
                            log.warn("Failed to set POSIX permissions for secret file {}: {}", targetFilePath, e.getMessage());
                        }
                    }
                }
            }

            // 2. Pull the required image
            pullImageIfNeeded(event.dockerImage());

            // 3. Configure mount
            // Use the Docker Compose-prefixed volume name (e.g. "rabotyagaci_app-data")
            Volume containerWorkspaceVolume = new Volume("/app-data");
            Bind workspaceBind = new Bind(runnerConfig.getVolumeName(), containerWorkspaceVolume);

            java.util.List<Bind> bindsList = new java.util.ArrayList<>();
            bindsList.add(workspaceBind);
            if (event.dockerSocket() || event.privileged()) {
                log.info("Mounting Docker socket for step: {}", event.stepName());
                bindsList.add(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")));
            }

            // 4. Configure host limits
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(bindsList.toArray(new Bind[0]))
                    .withMemory(parseMemoryLimit(runnerConfig.getContainerMemoryLimit()))
                    .withNanoCPUs(parseCpuLimit(runnerConfig.getContainerCpuLimit()))
                    .withNetworkMode(runnerConfig.getNetworkMode());

            if (event.privileged()) {
                log.info("Running step {} in privileged mode", event.stepName());
                hostConfig.withPrivileged(true);
            }

            // 5. Build commands shell script
            // Map the backend's internal path to the container's mounted volume path
            String containerWorkingDir = "/app-data/workspaces/" + event.workspaceDir().getFileName().toString();
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
            activeContainers.put(event.buildId(), containerId);

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
                                        String sanitizedLine = logSanitizer.sanitizeLine(event.stepId(), line);
                                        eventPublisher.publishEvent(new LogChunkEvent(
                                                event.buildId(),
                                                event.stepId(),
                                                streamType,
                                                sanitizedLine,
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
            // Flush and clean up log sanitizer session
            logSanitizer.flushSession(event.stepId());

            // Close log streaming adapters
            if (logCallback != null) {
                try {
                    logCallback.close();
                } catch (Exception ignored) {}
            }

            // Cleanup container
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                    log.info("Container {} removed", containerId);
                } catch (Exception e) {
                    log.warn("Failed to remove container {}", containerId, e);
                }
                activeContainers.remove(event.buildId());
            }

            buildSemaphore.release();

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

    private String resolveGitToken(dev.mathalama.rabotyagaci.project.domain.Project project) {
        if (project.getGithubToken() != null && !project.getGithubToken().isBlank()) {
            try {
                return secretCryptoService.decrypt(project.getGithubToken());
            } catch (Exception e) {
                log.error("Failed to decrypt github token for project {}", project.getName(), e);
            }
        }
        return null;
    }
}
