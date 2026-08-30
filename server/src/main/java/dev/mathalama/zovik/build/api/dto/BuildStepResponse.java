package dev.mathalama.zovik.build.api.dto;

import dev.mathalama.zovik.build.domain.StepStatus;

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
        Long durationMs,
        String dependsOn,
        int unresolvedDependenciesCount
) {}
