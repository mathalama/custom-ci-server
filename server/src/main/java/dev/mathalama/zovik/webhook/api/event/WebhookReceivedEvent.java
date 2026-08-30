package dev.mathalama.zovik.webhook.api.event;

import java.time.Instant;

/**
 * Published when a valid GitHub push webhook payload is received and verified.
 */
public record WebhookReceivedEvent(
        Long projectId,
        String branch,
        String commitSha,
        String commitMessage,
        String authorEmail,
        Instant receivedAt
) {}
