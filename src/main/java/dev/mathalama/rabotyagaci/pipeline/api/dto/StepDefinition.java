package dev.mathalama.rabotyagaci.pipeline.api.dto;

import java.util.List;

/**
 * Definition of a single build step parsed from YAML.
 */
public record StepDefinition(
        String name,
        String image,
        List<String> commands,
        List<String> artifacts,
        Boolean privileged,
        Boolean dockerSocket,
        List<SecretFileDefinition> secretFiles
) {
    public StepDefinition(String name, String image, List<String> commands, List<String> artifacts) {
        this(name, image, commands, artifacts, false, false, List.of());
    }

    public StepDefinition(String name, String image, List<String> commands, List<String> artifacts, Boolean privileged, Boolean dockerSocket) {
        this(name, image, commands, artifacts, privileged, dockerSocket, List.of());
    }
}
