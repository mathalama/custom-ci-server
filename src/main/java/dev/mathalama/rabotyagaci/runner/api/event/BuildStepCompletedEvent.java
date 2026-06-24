package dev.mathalama.rabotyagaci.runner.api.event;

import dev.mathalama.rabotyagaci.build.domain.StepStatus;
import java.time.Instant;

/**
 * Published by the runner when a build step completes execution.
 */
public record BuildStepCompletedEvent(
        Long buildId,
        Long stepId,
        StepStatus status,
        int exitCode,
        Instant completedAt
) {}
