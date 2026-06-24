package dev.mathalama.rabotyagaci.build.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TriggerBuildRequest(
        @NotBlank(message = "Branch name cannot be blank")
        String branch,

        @NotBlank(message = "Commit SHA cannot be blank")
        String commitSha
) {}
