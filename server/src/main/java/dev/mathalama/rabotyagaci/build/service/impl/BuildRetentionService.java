package dev.mathalama.rabotyagaci.build.service.impl;

import dev.mathalama.rabotyagaci.artifact.domain.BuildArtifact;
import dev.mathalama.rabotyagaci.artifact.repository.BuildArtifactRepository;
import dev.mathalama.rabotyagaci.build.config.RetentionConfig;
import dev.mathalama.rabotyagaci.build.domain.Build;
import dev.mathalama.rabotyagaci.build.repository.BuildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildRetentionService {

    private final BuildRepository buildRepository;
    private final BuildArtifactRepository buildArtifactRepository;
    private final RetentionConfig retentionConfig;

    @Value("${rabotyagaci.runner.workspace-dir}")
    private String workspaceDirParent;

    @Scheduled(cron = "${rabotyagaci.retention.cron}")
    @Transactional
    public void cleanupOldBuilds() {
        log.info("Starting scheduled build retention cleanup...");

        Instant threshold = Instant.now().minus(retentionConfig.getBuildsDays(), ChronoUnit.DAYS);
        List<Build> oldBuilds = buildRepository.findByCreatedAtBefore(threshold);

        if (oldBuilds.isEmpty()) {
            log.info("No old builds found to clean up.");
            return;
        }

        log.info("Found {} builds older than {} days. Starting cleanup...", oldBuilds.size(), retentionConfig.getBuildsDays());

        for (Build build : oldBuilds) {
            log.info("Cleaning up old build ID: {}", build.getId());

            // 1. Delete physical artifact files
            List<BuildArtifact> artifacts = buildArtifactRepository.findByBuildId(build.getId());
            for (BuildArtifact artifact : artifacts) {
                Path file = Path.of(artifact.getFilePath());
                if (Files.exists(file)) {
                    try {
                        Files.delete(file);
                        log.debug("Deleted old artifact file: {}", file);
                    } catch (IOException e) {
                        log.error("Failed to delete artifact file: {}", file, e);
                    }
                }
            }

            // 2. Delete workspace directory
            Path workspaceDir = Path.of(workspaceDirParent).resolve("build-" + build.getId());
            if (Files.exists(workspaceDir)) {
                try {
                    FileSystemUtils.deleteRecursively(workspaceDir);
                    log.debug("Deleted workspace directory for build ID: {}", build.getId());
                } catch (IOException e) {
                    log.error("Failed to delete workspace directory: {}", workspaceDir, e);
                }
            }

            // 3. Delete from DB (this automatically cascades to build_steps, build_logs, and build_artifacts)
            buildRepository.delete(build);
            log.info("Build ID {} completely deleted from DB and filesystem", build.getId());
        }

        log.info("Scheduled build retention cleanup completed successfully.");
    }
}
