package dev.mathalama.rabotyagaci.build.api.dto;

import dev.mathalama.rabotyagaci.build.domain.BuildStatus;
import dev.mathalama.rabotyagaci.build.domain.TriggerType;

import java.time.Instant;
import java.util.List;

public record BuildResponse(
        Long id,
        Long projectId,
        String commitSha,
        String branch,
        TriggerType triggerType,
        BuildStatus status,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        List<BuildStepResponse> steps
) {}
