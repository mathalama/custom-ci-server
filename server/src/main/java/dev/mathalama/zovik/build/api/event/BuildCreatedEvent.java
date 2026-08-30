package dev.mathalama.zovik.build.api.event;

import dev.mathalama.zovik.build.domain.TriggerType;
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
