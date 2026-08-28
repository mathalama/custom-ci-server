package dev.mathalama.rabotyagaci.runner.api.dto;

import dev.mathalama.rabotyagaci.build.domain.StepStatus;

public record RunnerCompleteJobRequest(
        int exitCode,
        StepStatus status
) {}
