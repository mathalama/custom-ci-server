package dev.mathalama.rabotyagaci.artifact.service.impl;

import dev.mathalama.rabotyagaci.artifact.config.ArtifactConfig;
import dev.mathalama.rabotyagaci.artifact.domain.BuildArtifact;
import dev.mathalama.rabotyagaci.artifact.repository.BuildArtifactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtifactCleanupService {

    private final BuildArtifactRepository buildArtifactRepository;
    private final ArtifactConfig artifactConfig;

    @Scheduled(cron = "0 0 3 * * *") // Every day at 3:00 AM
    @Transactional
    public void cleanupOldArtifacts() {
        log.info("Starting scheduled artifact cleanup...");

        Instant threshold = Instant.now().minus(artifactConfig.getRetentionDays(), ChronoUnit.DAYS);
        List<BuildArtifact> oldArtifacts = buildArtifactRepository.findByCreatedAtBefore(threshold);

        if (oldArtifacts.isEmpty()) {
            log.info("No old artifacts found to clean up.");
            return;
        }

        log.info("Found {} artifacts older than {} days. Deleting...", oldArtifacts.size(), artifactConfig.getRetentionDays());

        for (BuildArtifact artifact : oldArtifacts) {
            Path file = Path.of(artifact.getFilePath());
            if (Files.exists(file)) {
                try {
                    Files.delete(file);
                    log.info("Deleted artifact file: {}", file);
                } catch (IOException e) {
                    log.error("Failed to delete artifact file: {}", file, e);
                }
            }

            buildArtifactRepository.delete(artifact);
        }

        log.info("Scheduled artifact cleanup completed successfully.");
    }
}
