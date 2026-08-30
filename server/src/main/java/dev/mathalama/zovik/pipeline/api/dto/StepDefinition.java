package dev.mathalama.zovik.pipeline.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        List<SecretFileDefinition> secretFiles,
        @JsonProperty("depends_on") @JsonAlias({"dependsOn", "depends_on"}) List<String> dependsOn
) {
    public StepDefinition(String name, String image, List<String> commands, List<String> artifacts) {
        this(name, image, commands, artifacts, false, false, List.of(), List.of());
    }

    public StepDefinition(String name, String image, List<String> commands, List<String> artifacts, Boolean privileged, Boolean dockerSocket) {
        this(name, image, commands, artifacts, privileged, dockerSocket, List.of(), List.of());
    }

    public StepDefinition(String name, String image, List<String> commands, List<String> artifacts, Boolean privileged, Boolean dockerSocket, List<SecretFileDefinition> secretFiles) {
        this(name, image, commands, artifacts, privileged, dockerSocket, secretFiles, List.of());
    }
}
