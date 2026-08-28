package dev.mathalama.rabotyagaci.runner.api.dto;

import dev.mathalama.rabotyagaci.runner.domain.RunnerStatus;
import java.time.Instant;

public record RunnerResponse(
    Long id,
    String name,
    String host,
    Integer port,
    String username,
    RunnerStatus status,
    String os,
    Integer cpuCores,
    Long memoryBytes,
    String installLog,
    Instant lastSeenAt,
    Instant createdAt
) {}
