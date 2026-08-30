package dev.mathalama.zovik.runner.api.event;

import dev.mathalama.zovik.build.domain.StepStatus;
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
