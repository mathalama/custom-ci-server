package dev.mathalama.rabotyagaci.pipeline.service.impl;

import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.rabotyagaci.pipeline.api.dto.StepDefinition;
import dev.mathalama.rabotyagaci.pipeline.exception.PipelineParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelineServiceImplTest {

    private final PipelineServiceImpl pipelineService = new PipelineServiceImpl();

    @Test
    void parse_ShouldReturnDefinition_WhenYamlIsValid(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String validYaml = """
                version: 1
                name: "Test Build Pipeline"
                steps:
                  - name: "build-step"
                    image: "maven:3.9-eclipse-temurin-21"
                    commands:
                      - "mvn clean test"
                      - "mvn package"
                    artifacts:
                      - "target/*.jar"
                """;

        Files.writeString(tempDir.resolve(configPath), validYaml);

        PipelineDefinition result = pipelineService.parse(tempDir, configPath);

        assertNotNull(result);
        assertEquals(1, result.version());
        assertEquals("Test Build Pipeline", result.name());
        assertEquals(1, result.steps().size());

        StepDefinition step = result.steps().get(0);
        assertEquals("build-step", step.name());
        assertEquals("maven:3.9-eclipse-temurin-21", step.image());
        assertEquals(2, step.commands().size());
        assertEquals("mvn clean test", step.commands().get(0));
        assertEquals("mvn package", step.commands().get(1));
        assertEquals(1, step.artifacts().size());
        assertEquals("target/*.jar", step.artifacts().get(0));
    }

    @Test
    void parse_ShouldThrowException_WhenVersionIsMissing(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String invalidYaml = """
                name: "Missing Version"
                steps:
                  - name: "step"
                    image: "alpine"
                    commands:
                      - "echo"
                """;

        Files.writeString(tempDir.resolve(configPath), invalidYaml);

        assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
    }

    @Test
    void parse_ShouldThrowException_WhenStepsAreEmpty(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String invalidYaml = """
                version: 1
                name: "Empty Steps"
                steps: []
                """;

        Files.writeString(tempDir.resolve(configPath), invalidYaml);

        assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
    }

    @Test
    void parse_ShouldThrowException_WhenStepMissingImage(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String invalidYaml = """
                version: 1
                name: "Missing Image"
                steps:
                  - name: "step"
                    commands:
                      - "echo"
                """;

        Files.writeString(tempDir.resolve(configPath), invalidYaml);

        assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
    }

    @Test
    void parse_ShouldThrowException_WhenStepMissingCommands(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String invalidYaml = """
                version: 1
                name: "Missing Commands"
                steps:
                  - name: "step"
                    image: "alpine"
                """;

        Files.writeString(tempDir.resolve(configPath), invalidYaml);

        assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
    }
}
