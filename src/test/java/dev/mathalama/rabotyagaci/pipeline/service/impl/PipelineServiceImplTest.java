package dev.mathalama.rabotyagaci.pipeline.service.impl;

import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.rabotyagaci.pipeline.api.dto.StepDefinition;
import dev.mathalama.rabotyagaci.pipeline.config.PipelineConfig;
import dev.mathalama.rabotyagaci.pipeline.exception.PipelineParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceImplTest {

    @Mock
    private GitCloneService gitCloneService;

    @Mock
    private PipelineConfig pipelineConfig;

    @InjectMocks
    private PipelineServiceImpl pipelineService;

    @BeforeEach
    void setUp() {
        lenient().when(pipelineConfig.getCloneDir()).thenReturn(System.getProperty("java.io.tmpdir"));
    }

    @Test
    void parse_ShouldReturnDefinition_WhenYamlIsValid() {
        String repoUrl = "https://github.com/user/repo";
        String branch = "main";
        String commitSha = "d3b07384d113edec49eaa6238ad5ff00";
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

        when(gitCloneService.readFile(any(Path.class), eq(configPath))).thenReturn(validYaml);

        PipelineDefinition result = pipelineService.parse(repoUrl, branch, commitSha, configPath);

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

        verify(gitCloneService).cloneOrPull(eq(repoUrl), any(Path.class));
        verify(gitCloneService).checkoutCommit(any(Path.class), eq(commitSha));
    }

    @Test
    void parse_ShouldThrowException_WhenVersionIsMissing() {
        String configPath = ".rabotyaga.yaml";
        String invalidYaml = """
                name: "Missing Version"
                steps:
                  - name: "step"
                    image: "alpine"
                    commands:
                      - "echo"
                """;

        when(gitCloneService.readFile(any(Path.class), eq(configPath))).thenReturn(invalidYaml);

        assertThrows(PipelineParseException.class, () -> 
                pipelineService.parse("repo", "branch", "sha", configPath));
    }

    @Test
    void parse_ShouldThrowException_WhenStepsAreEmpty() {
        String configPath = ".rabotyaga.yaml";
        String invalidYaml = """
                version: 1
                name: "Empty Steps"
                steps: []
                """;

        when(gitCloneService.readFile(any(Path.class), eq(configPath))).thenReturn(invalidYaml);

        assertThrows(PipelineParseException.class, () -> 
                pipelineService.parse("repo", "branch", "sha", configPath));
    }

    @Test
    void parse_ShouldThrowException_WhenStepMissingImage() {
        String configPath = ".rabotyaga.yaml";
        String invalidYaml = """
                version: 1
                name: "Missing Image"
                steps:
                  - name: "step"
                    commands:
                      - "echo"
                """;

        when(gitCloneService.readFile(any(Path.class), eq(configPath))).thenReturn(invalidYaml);

        assertThrows(PipelineParseException.class, () -> 
                pipelineService.parse("repo", "branch", "sha", configPath));
    }

    @Test
    void parse_ShouldThrowException_WhenStepMissingCommands() {
        String configPath = ".rabotyaga.yaml";
        String invalidYaml = """
                version: 1
                name: "Missing Commands"
                steps:
                  - name: "step"
                    image: "alpine"
                """;

        when(gitCloneService.readFile(any(Path.class), eq(configPath))).thenReturn(invalidYaml);

        assertThrows(PipelineParseException.class, () -> 
                pipelineService.parse("repo", "branch", "sha", configPath));
    }
}
