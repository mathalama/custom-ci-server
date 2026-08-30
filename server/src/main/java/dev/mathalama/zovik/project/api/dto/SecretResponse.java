package dev.mathalama.zovik.project.api.dto;

import java.time.Instant;

public record SecretResponse(
        Long id,
        String name,
        String value,
        Instant createdAt
) {}
