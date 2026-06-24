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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtifactCollectorService {

    private final BuildArtifactRepository buildArtifactRepository;
    private final BuildRepository buildRepository;
    private final PipelineService pipelineService;
    private final ArtifactConfig artifactConfig;

    @Value("${rabotyagaci.runner.workspace-dir}")
    private String workspaceDirParent;

    @Async
    @EventListener
    @Transactional
    public void handleBuildCompleted(BuildCompletedEvent event) {
        log.info("Received BuildCompletedEvent for build ID: {}, status: {}", event.buildId(), event.status());

        if (event.status() != BuildStatus.SUCCESS) {
            log.info("Build {} did not succeed. Skipping artifact collection.", event.buildId());
            cleanupWorkspace(event.buildId());
            return;
        }

        Build build = buildRepository.findById(event.buildId()).orElse(null);
        if (build == null) {
            log.error("Build with ID: {} not found for artifact collection", event.buildId());
            return;
        }

        Project project = build.getProject();
        Path workspaceDir = Path.of(workspaceDirParent).resolve("build-" + build.getId());

        if (!Files.exists(workspaceDir)) {
            log.warn("Workspace directory {} does not exist. Skipping artifact collection.", workspaceDir);
            return;
        }

        try {
            // Parse pipeline config to get defined artifacts patterns
            PipelineDefinition pipelineDef = pipelineService.parse(
                    project.getRepoUrl(),
                    build.getBranch(),
                    build.getCommitSha(),
                    project.getPipelineConfigPath()
            );

            List<BuildArtifact> collectedArtifacts = new ArrayList<>();

            for (StepDefinition stepDef : pipelineDef.steps()) {
                if (stepDef.artifacts() == null || stepDef.artifacts().isEmpty()) {
                    continue;
                }

                log.info("Collecting artifacts for step '{}' using patterns: {}", stepDef.name(), stepDef.artifacts());

                for (String pattern : stepDef.artifacts()) {
                    List<Path> matchedFiles = findMatchingFiles(workspaceDir, pattern);
                    log.info("Pattern '{}' matched {} files", pattern, matchedFiles.size());

                    for (Path file : matchedFiles) {
                        Path storageDir = Path.of(artifactConfig.getStorageDir()).resolve("build-" + build.getId());
                        Files.createDirectories(storageDir);

                        Path destFile = storageDir.resolve(file.getFileName());
                        Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);

                        String contentType = Files.probeContentType(file);
                        if (contentType == null) {
                            contentType = "application/octet-stream";
                        }

                        BuildArtifact artifact = BuildArtifact.builder()
                                .build(build)
                                .fileName(file.getFileName().toString())
                                .filePath(destFile.toAbsolutePath().toString())
                                .fileSize(Files.size(file))
                                .contentType(contentType)
                                .build();

                        collectedArtifacts.add(artifact);
                        log.info("Collected artifact: {} (size: {} bytes)", artifact.getFileName(), artifact.getFileSize());
                    }
                }
            }

            if (!collectedArtifacts.isEmpty()) {
                buildArtifactRepository.saveAll(collectedArtifacts);
                log.info("Successfully saved {} artifacts to database for build ID: {}", collectedArtifacts.size(), build.getId());
            }

        } catch (Exception e) {
            log.error("Failed to collect artifacts for build ID: {}", build.getId(), e);
        } finally {
            cleanupWorkspace(build.getId());
        }
    }

    private List<Path> findMatchingFiles(Path baseDir, String pattern) throws IOException {
        List<Path> matches = new ArrayList<>();
        String globPattern = pattern.replace("\\", "/");

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(
                "glob:" + baseDir.toAbsolutePath().toString().replace("\\", "/") + "/" + globPattern
        );

        try (Stream<Path> stream = Files.walk(baseDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String pathStr = path.toAbsolutePath().toString().replace("\\", "/");
                        return matcher.matches(Path.of(pathStr));
                    })
                    .forEach(matches::add);
        }
        return matches;
    }

    private void cleanupWorkspace(Long buildId) {
        Path workspaceDir = Path.of(workspaceDirParent).resolve("build-" + buildId);
        if (Files.exists(workspaceDir)) {
            log.info("Cleaning up workspace directory: {}", workspaceDir);
            try (Stream<Path> stream = Files.walk(workspaceDir)) {
                stream.sorted((p1, p2) -> p2.compareTo(p1)) // delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Failed to delete path: {}", path, e);
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to clean up workspace for build: {}", buildId, e);
            }
        }
    }
}
