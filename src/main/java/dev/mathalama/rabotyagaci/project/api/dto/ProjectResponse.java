package dev.mathalama.rabotyagaci.project.api.dto;

import dev.mathalama.rabotyagaci.project.domain.GitProvider;
import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String repoUrl,
        GitProvider gitProvider,
        String defaultBranch,
        String webhookSecret,
        String pipelineConfigPath,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}
