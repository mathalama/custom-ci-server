package dev.mathalama.rabotyagaci.pipeline.api;

import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;

public interface PipelineService {

    /**
     * Clones the repository, checkouts specific commit, and parses the pipeline configuration file.
     *
     * @param repoUrl   the Git repository URL (HTTPS or SSH)
     * @param branch    the branch name
     * @param commitSha the commit hash (SHA-1)
     * @param configFilePath the path to the configuration file (e.g. .rabotyaga.yaml)
     * @return parsed PipelineDefinition
     * @throws dev.mathalama.rabotyagaci.pipeline.exception.PipelineParseException if YAML is invalid or parsing fails
     */
    PipelineDefinition parse(String repoUrl, String branch, String commitSha, String configFilePath);
}
