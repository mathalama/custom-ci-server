package dev.mathalama.zovik.pipeline.service.impl;

import dev.mathalama.zovik.pipeline.api.PipelineService;
import dev.mathalama.zovik.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.zovik.pipeline.api.dto.StepDefinition;
import dev.mathalama.zovik.pipeline.exception.PipelineParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
public class PipelineServiceImpl implements PipelineService {

    @Override
    public PipelineDefinition parse(Path projectDir, String configFilePath) {
        log.info("Starting pipeline parsing from local project dir: {}, file: {}", projectDir, configFilePath);

        Path file = projectDir.resolve(configFilePath);
        if (!Files.exists(file)) {
            throw new PipelineParseException("Configuration file not found in repository: " + configFilePath);
        }

        try {
            String yamlContent = Files.readString(file);

            // Parse YAML content to PipelineDefinition using Jackson 3 YAMLMapper
            YAMLMapper yamlMapper = new YAMLMapper();
            PipelineDefinition pipelineDefinition = yamlMapper.readValue(yamlContent, PipelineDefinition.class);

            // Validate parsed definition structure & DAG dependencies
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

        Set<String> stepNames = new HashSet<>();
        for (int i = 0; i < pipeline.steps().size(); i++) {
            StepDefinition step = pipeline.steps().get(i);
            if (step == null) {
                throw new PipelineParseException("Step at index " + i + " cannot be null");
            }
            if (step.name() == null || step.name().isBlank()) {
                throw new PipelineParseException("Step name at index " + i + " cannot be blank");
            }
            if (!stepNames.add(step.name())) {
                throw new PipelineParseException("Duplicate step name found: '" + step.name() + "'");
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

        // DAG cycle and reference validation
        Map<String, List<String>> adjList = new HashMap<>(); // parent -> children
        Map<String, Integer> inDegree = new HashMap<>();

        for (StepDefinition step : pipeline.steps()) {
            adjList.putIfAbsent(step.name(), new ArrayList<>());
            int depsCount = 0;
            if (step.dependsOn() != null) {
                for (String dep : step.dependsOn()) {
                    if (dep == null || dep.isBlank()) continue;
                    if (dep.equals(step.name())) {
                        throw new PipelineParseException("Step '" + step.name() + "' cannot depend on itself");
                    }
                    if (!stepNames.contains(dep)) {
                        throw new PipelineParseException("Step '" + step.name() + "' depends on unknown step: '" + dep + "'");
                    }
                    adjList.computeIfAbsent(dep, k -> new ArrayList<>()).add(step.name());
                    depsCount++;
                }
            }
            inDegree.put(step.name(), depsCount);
        }

        // Kahn's Algorithm for Topological Sort & Cycle Detection
        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int visitedCount = 0;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            visitedCount++;
            List<String> children = adjList.getOrDefault(current, Collections.emptyList());
            for (String child : children) {
                int remaining = inDegree.get(child) - 1;
                inDegree.put(child, remaining);
                if (remaining == 0) {
                    queue.add(child);
                }
            }
        }

        if (visitedCount != pipeline.steps().size()) {
            throw new PipelineParseException("Cyclic dependency detected in pipeline steps");
        }
    }
}
