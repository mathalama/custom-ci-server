package dev.mathalama.zovik.runner.api.dto;

public record RunnerRegisterResponse(
        Long runnerId,
        String runnerToken,
        String name
) {}
