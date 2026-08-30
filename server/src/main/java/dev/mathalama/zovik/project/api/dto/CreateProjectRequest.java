package dev.mathalama.zovik.project.api.dto;

import dev.mathalama.zovik.project.domain.GitProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectRequest(
        @NotBlank(message = "Project name cannot be blank")
        String name,

        @NotBlank(message = "Repository URL cannot be blank")
        String repoUrl,

        @NotNull(message = "Git provider must be specified")
        GitProvider gitProvider,

        String defaultBranch,

        String pipelineConfigPath,

        String githubToken
) {}
