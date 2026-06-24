package dev.mathalama.rabotyagaci.runner.api.event;

import java.time.Instant;

/**
 * Published by the runner for each line of stdout/stderr from a running build step container.
 */
public record LogChunkEvent(
        Long buildStepId,
        String stream, // "STDOUT" or "STDERR"
        String content,
        int lineNumber,
        Instant timestamp
) {}
