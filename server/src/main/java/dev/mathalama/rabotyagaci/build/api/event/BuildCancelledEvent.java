package dev.mathalama.rabotyagaci.build.api.event;

import java.time.Instant;

/**
 * Triggered when a running build is cancelled by the user.
 */
public record BuildCancelledEvent(
        Long buildId,
        Instant cancelledAt
) {}
