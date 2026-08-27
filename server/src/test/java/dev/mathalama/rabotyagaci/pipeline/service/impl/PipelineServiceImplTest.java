package dev.mathalama.rabotyagaci.pipeline.service.impl;

import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.rabotyagaci.pipeline.api.dto.StepDefinition;
import dev.mathalama.rabotyagaci.pipeline.exception.PipelineParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PipelineServiceImplTest {

    private final PipelineServiceImpl pipelineService = new PipelineServiceImpl();

    @Test
    @DisplayName("Should parse valid sequential pipeline YAML")
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
    @DisplayName("Should parse valid DAG pipeline with parallel branches and dependencies")
    void parse_ShouldParseValidDagPipeline(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String dagYaml = """
                version: 1
                name: "DAG Multi-Branch Pipeline"
                steps:
                  - name: "checkout-and-build"
                    image: "maven:3.9-eclipse-temurin-21"
                    commands:
                      - "mvn compile"

                  - name: "unit-tests"
                    image: "maven:3.9-eclipse-temurin-21"
                    depends_on: ["checkout-and-build"]
                    commands:
                      - "mvn test"

                  - name: "lint"
                    image: "node:20-alpine"
                    depends_on: ["checkout-and-build"]
                    commands:
                      - "npm run lint"

                  - name: "deploy-staging"
                    image: "docker:stable"
                    depends_on: ["unit-tests", "lint"]
                    commands:
                      - "docker push myapp:latest"
                """;

        Files.writeString(tempDir.resolve(configPath), dagYaml);

        PipelineDefinition result = pipelineService.parse(tempDir, configPath);

        assertNotNull(result);
        assertEquals(4, result.steps().size());
        assertEquals("checkout-and-build", result.steps().get(0).name());
        assertEquals(1, result.steps().get(1).dependsOn().size());
        assertEquals("checkout-and-build", result.steps().get(1).dependsOn().get(0));
        assertEquals(2, result.steps().get(3).dependsOn().size());
    }

    @Test
    @DisplayName("Should throw exception when circular dependency is detected (A -> B -> A)")
    void parse_ShouldThrowException_WhenDirectCycleDetected(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String cycleYaml = """
                version: 1
                steps:
                  - name: "stepA"
                    image: "alpine"
                    depends_on: ["stepB"]
                    commands: ["echo A"]

                  - name: "stepB"
                    image: "alpine"
                    depends_on: ["stepA"]
                    commands: ["echo B"]
                """;

        Files.writeString(tempDir.resolve(configPath), cycleYaml);

        PipelineParseException ex = assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
        assertTrue(ex.getMessage().contains("Cyclic dependency"));
    }

    @Test
    @DisplayName("Should throw exception when circular dependency is detected (A -> B -> C -> A)")
    void parse_ShouldThrowException_WhenTransitiveCycleDetected(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String cycleYaml = """
                version: 1
                steps:
                  - name: "stepA"
                    image: "alpine"
                    depends_on: ["stepC"]
                    commands: ["echo A"]

                  - name: "stepB"
                    image: "alpine"
                    depends_on: ["stepA"]
                    commands: ["echo B"]

                  - name: "stepC"
                    image: "alpine"
                    depends_on: ["stepB"]
                    commands: ["echo C"]
                """;

        Files.writeString(tempDir.resolve(configPath), cycleYaml);

        PipelineParseException ex = assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
        assertTrue(ex.getMessage().contains("Cyclic dependency"));
    }

    @Test
    @DisplayName("Should throw exception when step depends on itself")
    void parse_ShouldThrowException_WhenStepDependsOnItself(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String selfDepYaml = """
                version: 1
                steps:
                  - name: "stepSelf"
                    image: "alpine"
                    depends_on: ["stepSelf"]
                    commands: ["echo Self"]
                """;

        Files.writeString(tempDir.resolve(configPath), selfDepYaml);

        PipelineParseException ex = assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
        assertTrue(ex.getMessage().contains("cannot depend on itself"));
    }

    @Test
    @DisplayName("Should throw exception when step depends on non-existent step")
    void parse_ShouldThrowException_WhenStepDependsOnUnknownStep(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String unknownDepYaml = """
                version: 1
                steps:
                  - name: "step1"
                    image: "alpine"
                    depends_on: ["nonExistentStep"]
                    commands: ["echo 1"]
                """;

        Files.writeString(tempDir.resolve(configPath), unknownDepYaml);

        PipelineParseException ex = assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
        assertTrue(ex.getMessage().contains("depends on unknown step"));
    }

    @Test
    @DisplayName("Should throw exception when duplicate step names exist")
    void parse_ShouldThrowException_WhenDuplicateStepName(@TempDir Path tempDir) throws IOException {
        String configPath = ".rabotyaga.yaml";
        String dupYaml = """
                version: 1
                steps:
                  - name: "same-name"
                    image: "alpine"
                    commands: ["echo 1"]
                  - name: "same-name"
                    image: "alpine"
                    commands: ["echo 2"]
                """;

        Files.writeString(tempDir.resolve(configPath), dupYaml);

        PipelineParseException ex = assertThrows(PipelineParseException.class, () ->
                pipelineService.parse(tempDir, configPath));
        assertTrue(ex.getMessage().contains("Duplicate step name"));
    }
}
