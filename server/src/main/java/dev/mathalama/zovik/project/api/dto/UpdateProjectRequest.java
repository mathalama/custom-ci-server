package dev.mathalama.zovik.project.api.dto;

import dev.mathalama.zovik.project.domain.GitProvider;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(min = 1, message = "Project name must not be empty")
        String name,

        String repoUrl,
        GitProvider gitProvider,
        String defaultBranch,
        String pipelineConfigPath,
        Boolean isActive,
        String githubToken,
        Boolean clearGithubToken
) {}
