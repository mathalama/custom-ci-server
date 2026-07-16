package dev.mathalama.rabotyagaci.pipeline.service.impl;

import dev.mathalama.rabotyagaci.pipeline.api.PipelineService;
import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.rabotyagaci.pipeline.api.dto.StepDefinition;
import dev.mathalama.rabotyagaci.pipeline.config.PipelineConfig;
import dev.mathalama.rabotyagaci.pipeline.exception.PipelineParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class PipelineServiceImpl implements PipelineService {

    @Override
    public PipelineDefinition parse(Path projectDir, String configFilePath) {
        log.info("Starting pipeline parsing from local project dir: {}, file: {}", projectDir, configFilePath);

        Path file = projectDir.resolve(configFilePath);
        if (!java.nio.file.Files.exists(file)) {
            throw new PipelineParseException("Configuration file not found in repository: " + configFilePath);
        }

        try {
            String yamlContent = java.nio.file.Files.readString(file);

            // Parse YAML content to PipelineDefinition using Jackson 3 YAMLMapper
            YAMLMapper yamlMapper = new YAMLMapper();
            PipelineDefinition pipelineDefinition = yamlMapper.readValue(yamlContent, PipelineDefinition.class);

            // Validate parsed definition structure
            validatePipeline(pipelineDefinition);

            log.info("Pipeline configuration parsed and validated successfully. Steps count: {}",
                    pipelineDefinition.steps().size());
            return pipelineDefinition;

        } catch (PipelineParseException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineParseException("Failed to parse and validate pipeline config: " + e.getMessage(), e);
        }
    }

    private void validatePipeline(PipelineDefinition pipeline) {
        if (pipeline == null) {
            throw new PipelineParseException("Pipeline configuration is empty");
        }
        if (pipeline.version() <= 0) {
            throw new PipelineParseException("Pipeline version must be greater than 0");
        }
        if (pipeline.steps() == null || pipeline.steps().isEmpty()) {
            throw new PipelineParseException("Pipeline must contain at least one step");
        }

        for (int i = 0; i < pipeline.steps().size(); i++) {
            StepDefinition step = pipeline.steps().get(i);
            if (step == null) {
                throw new PipelineParseException("Step at index " + i + " cannot be null");
            }
            if (step.name() == null || step.name().isBlank()) {
                throw new PipelineParseException("Step name at index " + i + " cannot be blank");
            }
            if (step.image() == null || step.image().isBlank()) {
                throw new PipelineParseException("Docker image for step '" + step.name() + "' cannot be blank");
            }
            if (step.commands() == null || step.commands().isEmpty()) {
                throw new PipelineParseException("Step '" + step.name() + "' must specify at least one command");
            }
            for (int cmdIdx = 0; cmdIdx < step.commands().size(); cmdIdx++) {
                String cmd = step.commands().get(cmdIdx);
                if (cmd == null || cmd.isBlank()) {
                    throw new PipelineParseException("Step '" + step.name() + "' contains a blank command at index " + cmdIdx);
                }
            }
        }
    }
}
