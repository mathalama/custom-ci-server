package dev.mathalama.rabotyagaci.runner.api.dto;

public record RunnerRegisterResponse(
        Long runnerId,
        String runnerToken,
        String name
) {}
