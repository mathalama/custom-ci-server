package dev.mathalama.rabotyagaci.pipeline.api;

import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;

public interface PipelineService {

    /**
     * Parses the pipeline configuration file from the local repository directory.
     *
     * @param projectDir     the local workspace directory containing the code
     * @param configFilePath the path to the configuration file (e.g. .rabotyaga.yaml)
     * @return parsed PipelineDefinition
     * @throws dev.mathalama.rabotyagaci.pipeline.exception.PipelineParseException if YAML is invalid or parsing fails
     */
    PipelineDefinition parse(java.nio.file.Path projectDir, String configFilePath);
}
