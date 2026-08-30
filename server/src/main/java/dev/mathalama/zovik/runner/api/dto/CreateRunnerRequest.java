package dev.mathalama.zovik.runner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRunnerRequest(
    @NotBlank String name,
    @NotBlank String host,
    @NotNull int port,
    @NotBlank String username,
    String password,
    String sshKey
) {}
