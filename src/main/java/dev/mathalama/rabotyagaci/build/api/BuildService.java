package dev.mathalama.rabotyagaci.build.api;

import dev.mathalama.rabotyagaci.build.api.dto.BuildResponse;
import dev.mathalama.rabotyagaci.build.api.dto.TriggerBuildRequest;
import dev.mathalama.rabotyagaci.build.domain.StepStatus;
import dev.mathalama.rabotyagaci.build.domain.TriggerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Cancels a running build.
     */
    void cancel(Long id);

    /**
     * Callback method called when a build step completes execution.
     */
    void onStepCompleted(Long buildId, Long stepId, StepStatus status, int exitCode);
}
