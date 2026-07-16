package dev.mathalama.rabotyagaci.build.service.impl;

import dev.mathalama.rabotyagaci.build.api.BuildService;
import dev.mathalama.rabotyagaci.pipeline.api.PipelineService;
import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.rabotyagaci.pipeline.service.impl.GitCloneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildOrchestrator {

    private final GitCloneService gitCloneService;
    private final PipelineService pipelineService;
    private final BuildService buildService;

    @Value("${rabotyagaci.runner.workspace-dir}")
    private String workspaceDirParent;

    @Value("${rabotyagaci.project.pipeline-config-path:.rabotyaga.yaml}")
    private String defaultPipelineConfigPath;

    @Async
    public void executeBuildAsync(Long buildId, String repoUrl, String branch, String commitSha, String configPath) {
        log.info("Starting asynchronous build orchestration for build ID: {}", buildId);

        Path workspaceDir = Path.of(workspaceDirParent).resolve("build-" + buildId);
        String pipelinePath = (configPath != null && !configPath.isBlank()) ? configPath : defaultPipelineConfigPath;

        try {
            log.info("Preparing workspace and cloning code into: {}", workspaceDir);
            gitCloneService.cloneOrPull(repoUrl, workspaceDir);

            String ref = (commitSha != null && !commitSha.isBlank()) ? commitSha : branch;
            gitCloneService.checkoutCommit(workspaceDir, ref);

            log.info("Parsing pipeline configuration file: {}", pipelinePath);
            PipelineDefinition pipelineDef = pipelineService.parse(workspaceDir, pipelinePath);

            log.info("Initializing build steps in DB for build ID: {}", buildId);
            buildService.prepareBuildSteps(buildId, pipelineDef);

            log.info("Starting build execution flow for build ID: {}", buildId);
            buildService.startNextStep(buildId);

        } catch (Exception e) {
            log.error("Fatal error during build orchestration for build ID: {}", buildId, e);
            try {
                buildService.failBuild(buildId, e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to mark build ID: {} as failed", buildId, ex);
            }
        }
    }
}
