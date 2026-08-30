package dev.mathalama.zovik.runner.service.impl;

import com.github.dockerjava.api.DockerClient;
import dev.mathalama.zovik.build.api.event.BuildStepStartedEvent;
import dev.mathalama.zovik.build.domain.StepStatus;
import dev.mathalama.zovik.runner.api.event.BuildStepCompletedEvent;
import dev.mathalama.zovik.runner.api.event.LogChunkEvent;
import dev.mathalama.zovik.runner.config.RunnerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DockerRunnerServiceTest {

    private DockerClient dockerClient;
    private RunnerConfig runnerConfig;
    private ApplicationEventPublisher eventPublisher;
    private DockerRunnerService dockerRunnerService;

    @BeforeEach
    void setUp() {
        runnerConfig = new RunnerConfig();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            runnerConfig.setDockerHost("npipe:////./pipe/docker_engine");
        } else {
            runnerConfig.setDockerHost("unix:///var/run/docker.sock");
        }
        runnerConfig.setDefaultTimeout(30L);
        runnerConfig.setContainerMemoryLimit("256m");
        runnerConfig.setContainerCpuLimit(0.5);
        runnerConfig.setNetworkMode("bridge");

        DockerClientFactory factory = new DockerClientFactory(runnerConfig);
        dockerClient = factory.dockerClient();

        eventPublisher = mock(ApplicationEventPublisher.class);
        
        var runnerRepository = mock(dev.mathalama.zovik.runner.repository.RunnerRepository.class);
        when(runnerRepository.findAll()).thenReturn(java.util.List.of());
        
        var runnerWebSocketHandler = mock(dev.mathalama.zovik.runner.websocket.RunnerWebSocketHandler.class);
        var runnerJobQueueService = mock(RunnerJobQueueService.class);
        var buildRepository = mock(dev.mathalama.zovik.build.repository.BuildRepository.class);
        var buildStepRepository = mock(dev.mathalama.zovik.build.repository.BuildStepRepository.class);
        var secretCryptoService = mock(dev.mathalama.zovik.project.service.impl.SecretCryptoService.class);
        var logSanitizer = new dev.mathalama.zovik.log.service.LogSanitizer();

        dockerRunnerService = new DockerRunnerService(
            dockerClient, 
            runnerConfig, 
            eventPublisher,
            runnerRepository,
            runnerWebSocketHandler,
            runnerJobQueueService,
            buildRepository,
            buildStepRepository,
            secretCryptoService,
            logSanitizer
        );
        dockerRunnerService.init();
    }

    @Test
    void testExecuteSimpleStep(@TempDir Path tempDir) throws IOException {
        try {
            dockerClient.pingCmd().exec();
        } catch (Exception e) {
            System.out.println("Docker is not running, skipping integration test: " + e.getMessage());
            return;
        }

        runnerConfig.setVolumeName(tempDir.toAbsolutePath().toString().replace("\\", "/"));
        Path workspacesDir = tempDir.resolve("workspaces");
        Path workspaceDir = workspacesDir.resolve("workspace");
        Files.createDirectories(workspaceDir);

        BuildStepStartedEvent event = new BuildStepStartedEvent(
                1L,
                10L,
                "Test Step",
                "alpine:latest",
                List.of("echo 'Hello from Zovik'", "echo 'Line 2'"),
                workspaceDir,
                Map.of(),
                false,
                false,
                Map.of()
        );

        dockerRunnerService.handleBuildStepStarted(event);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        List<Object> capturedEvents = eventCaptor.getAllValues();

        List<LogChunkEvent> logChunks = new ArrayList<>();
        BuildStepCompletedEvent completedEvent = null;

        for (Object capEvent : capturedEvents) {
            if (capEvent instanceof LogChunkEvent logChunk) {
                logChunks.add(logChunk);
            } else if (capEvent instanceof BuildStepCompletedEvent compEvent) {
                completedEvent = compEvent;
            }
        }

        assertNotNull(completedEvent, "BuildStepCompletedEvent should be published");
        assertEquals(1L, completedEvent.buildId());
        assertEquals(10L, completedEvent.stepId());
        assertEquals(StepStatus.SUCCESS, completedEvent.status());
        assertEquals(0, completedEvent.exitCode());

        assertFalse(logChunks.isEmpty(), "Log chunks should not be empty");
        boolean foundHello = logChunks.stream()
                .anyMatch(chunk -> chunk.content().contains("Hello from Zovik"));
        assertTrue(foundHello, "Should capture log output");
    }

    @Test
    void testExecuteFailureStep(@TempDir Path tempDir) throws IOException {
        try {
            dockerClient.pingCmd().exec();
        } catch (Exception e) {
            System.out.println("Docker is not running, skipping integration test: " + e.getMessage());
            return;
        }

        runnerConfig.setVolumeName(tempDir.toAbsolutePath().toString().replace("\\", "/"));
        Path workspacesDir = tempDir.resolve("workspaces");
        Path workspaceDir = workspacesDir.resolve("workspace");
        Files.createDirectories(workspaceDir);

        BuildStepStartedEvent event = new BuildStepStartedEvent(
                1L,
                11L,
                "Fail Step",
                "alpine:latest",
                List.of("exit 42"),
                workspaceDir,
                Map.of(),
                false,
                false,
                Map.of()
        );

        dockerRunnerService.handleBuildStepStarted(event);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        BuildStepCompletedEvent completedEvent = eventCaptor.getAllValues().stream()
                .filter(BuildStepCompletedEvent.class::isInstance)
                .map(BuildStepCompletedEvent.class::cast)
                .findFirst()
                .orElse(null);

        assertNotNull(completedEvent, "BuildStepCompletedEvent should be published");
        assertEquals(StepStatus.FAILURE, completedEvent.status());
        assertEquals(42, completedEvent.exitCode());
    }
}
