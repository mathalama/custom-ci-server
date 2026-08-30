package dev.mathalama.zovik.build.api.event;

import dev.mathalama.zovik.build.domain.BuildStatus;
import java.time.Instant;

/**
 * Triggered when the entire build process completes (success, failure, or cancellation).
 */
public record BuildCompletedEvent(
        Long buildId,
        BuildStatus status,
        Instant completedAt
) {}
