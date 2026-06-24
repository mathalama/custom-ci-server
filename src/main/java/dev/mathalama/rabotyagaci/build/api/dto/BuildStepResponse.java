package dev.mathalama.rabotyagaci.build.api.dto;

import dev.mathalama.rabotyagaci.build.domain.StepStatus;

import java.time.Instant;

public record BuildStepResponse(
        Long id,
        String name,
        int stepOrder,
        String dockerImage,
        String commands,
        StepStatus status,
        Integer exitCode,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {}
