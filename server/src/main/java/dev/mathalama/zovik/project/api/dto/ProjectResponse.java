package dev.mathalama.zovik.project.api.dto;

import dev.mathalama.zovik.project.domain.GitProvider;
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
        String githubToken,
        Instant createdAt,
        Instant updatedAt
) {}
