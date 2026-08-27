package dev.mathalama.rabotyagaci.build.api.event;

import dev.mathalama.rabotyagaci.build.domain.TriggerType;
import java.time.Instant;

/**
 * Triggered when a new build is created in the system.
 */
public record BuildCreatedEvent(
        Long buildId,
        Long projectId,
        TriggerType triggerType,
        Instant createdAt
) {}
