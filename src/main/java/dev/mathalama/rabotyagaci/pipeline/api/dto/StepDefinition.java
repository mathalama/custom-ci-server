package dev.mathalama.rabotyagaci.pipeline.api.dto;

import java.util.List;

/**
 * Definition of a single build step parsed from YAML.
 */
public record StepDefinition(
        String name,
        String image,
        List<String> commands,
        List<String> artifacts
) {}
