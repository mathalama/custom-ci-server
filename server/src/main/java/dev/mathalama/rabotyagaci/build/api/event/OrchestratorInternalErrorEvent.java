package dev.mathalama.rabotyagaci.build.api.event;

import java.time.Instant;

/**
 * Triggered when a critical uncaught exception occurs in the asynchronous event-driven architecture,
 * particularly to prevent hanging (zombie) builds.
 */
public record OrchestratorInternalErrorEvent(
        Long buildId,
        Throwable cause,
        Instant occurredAt
) {}
