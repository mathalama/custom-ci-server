package dev.mathalama.zovik.pipeline.api.dto;

import java.util.List;

/**
 * Definition of the build pipeline parsed from YAML.
 */
public record PipelineDefinition(
        int version,
        String name,
        List<StepDefinition> steps,
        NotificationsDefinition notifications,
        CacheDefinition cache
) {}
