package dev.mathalama.zovik.runner.api.dto;

import dev.mathalama.zovik.build.domain.StepStatus;

public record RunnerCompleteJobRequest(
        int exitCode,
        StepStatus status
) {}
