package dev.mathalama.rabotyagaci.artifact.service.impl;

import dev.mathalama.rabotyagaci.artifact.config.ArtifactConfig;
import dev.mathalama.rabotyagaci.artifact.domain.BuildArtifact;
import dev.mathalama.rabotyagaci.artifact.repository.BuildArtifactRepository;
import dev.mathalama.rabotyagaci.build.api.event.BuildCompletedEvent;
import dev.mathalama.rabotyagaci.build.domain.Build;
import dev.mathalama.rabotyagaci.build.domain.BuildStatus;
import dev.mathalama.rabotyagaci.build.repository.BuildRepository;
import dev.mathalama.rabotyagaci.pipeline.api.PipelineService;
import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.rabotyagaci.pipeline.api.dto.StepDefinition;
import dev.mathalama.rabotyagaci.project.domain.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactCollectorServiceTest {

    @Mock
    private BuildArtifactRepository buildArtifactRepository;

    @Mock
    private BuildRepository buildRepository;

    @Mock
    private PipelineService pipelineService;

    @Mock
    private ArtifactConfig artifactConfig;

    @InjectMocks
    private ArtifactCollectorService artifactCollectorService;

    private Build testBuild;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id(1L)
                .repoUrl("https://github.com/test/repo")
                .pipelineConfigPath(".rabotyaga.yaml")
                .build();

        testBuild = Build.builder()
                .id(5L)
                .project(testProject)
                .branch("main")
                .commitSha("123456")
                .status(BuildStatus.SUCCESS)
                .build();
    }

    @Test
    void testCollectArtifactsSuccess(@TempDir Path tempDir) throws IOException {
        Path workspaceParent = tempDir.resolve("workspaces");
        Path storageDir = tempDir.resolve("storage");
        Files.createDirectories(workspaceParent);
        Files.createDirectories(storageDir);

        ReflectionTestUtils.setField(artifactCollectorService, "workspaceDirParent", workspaceParent.toString());
        when(artifactConfig.getStorageDir()).thenReturn(storageDir.toString());

        Path buildWorkspace = workspaceParent.resolve("build-5");
        Files.createDirectories(buildWorkspace);
        Path targetDir = buildWorkspace.resolve("target");
        Files.createDirectories(targetDir);

        Path artifactFile = targetDir.resolve("app.jar");
        Files.writeString(artifactFile, "fake jar content");

        PipelineDefinition pipelineDef = new PipelineDefinition(
                1,
                "Test Pipeline",
                List.of(new StepDefinition("Build", "maven", List.of("mvn package"), List.of("target/*.jar"))),
                null,
                null
        );

        when(buildRepository.findById(5L)).thenReturn(Optional.of(testBuild));
        when(pipelineService.parse(any(), any(), any(), any())).thenReturn(pipelineDef);

        BuildCompletedEvent event = new BuildCompletedEvent(5L, BuildStatus.SUCCESS, Instant.now());
        artifactCollectorService.handleBuildCompleted(event);

        verify(buildArtifactRepository, times(1)).saveAll(anyList());

        // Workspace should be cleaned up
        assertFalse(Files.exists(buildWorkspace));

        // File should be stored in the storage directory
        Path storedFile = storageDir.resolve("build-5").resolve("app.jar");
        assertTrue(Files.exists(storedFile));
    }
}
