package dev.mathalama.rabotyagaci.build.api;

import dev.mathalama.rabotyagaci.build.api.dto.BuildResponse;
import dev.mathalama.rabotyagaci.build.api.dto.TriggerBuildRequest;
import dev.mathalama.rabotyagaci.build.domain.StepStatus;
import dev.mathalama.rabotyagaci.build.domain.TriggerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BuildService {

    /**
     * Triggers a new build asynchronously.
     */
    BuildResponse trigger(Long projectId, TriggerBuildRequest request, TriggerType type);

    /**
     * Fetches build details by ID.
     */
    BuildResponse getById(Long id);

    /**
     * Fetches builds of a project with pagination.
     */
    Page<BuildResponse> getAll(Long projectId, Pageable pageable);

    /**
     * Fetches recent builds across all projects (optimized, no N+1).
     */
    List<BuildResponse> getRecentBuilds(int limit);

    /**
     * Cancels a running build.
     */
    void cancel(Long id);

    /**
     * Callback method called when a build step completes execution.
     */
    void onStepCompleted(Long buildId, Long stepId, StepStatus status, int exitCode);

    /**
     * Transactional method to set build status to RUNNING and populate build steps.
     */
    void prepareBuildSteps(Long buildId, dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition pipelineDef);

    /**
     * Transactional method to find and execute the next pending step of a build.
     */
    void startNextStep(Long buildId);

    /**
     * Transactional method to mark a build as failed (e.g. if clone or parsing failed).
     */
    void failBuild(Long buildId, String errorMessage);
}
