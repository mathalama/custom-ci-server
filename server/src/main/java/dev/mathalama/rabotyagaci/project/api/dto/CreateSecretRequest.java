package dev.mathalama.rabotyagaci.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateSecretRequest(
        @NotBlank(message = "Secret name cannot be blank")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Secret name can only contain letters, numbers, and underscores")
        String name,

        @NotBlank(message = "Secret value cannot be blank")
        String value
) {}
