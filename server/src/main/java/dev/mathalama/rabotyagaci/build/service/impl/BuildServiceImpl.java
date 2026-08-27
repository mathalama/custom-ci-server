package dev.mathalama.rabotyagaci.build.service.impl;

import dev.mathalama.rabotyagaci.build.api.BuildService;
import dev.mathalama.rabotyagaci.build.api.dto.BuildResponse;
import dev.mathalama.rabotyagaci.build.api.dto.TriggerBuildRequest;
import dev.mathalama.rabotyagaci.build.api.event.BuildCancelledEvent;
import dev.mathalama.rabotyagaci.build.api.event.BuildCompletedEvent;
import dev.mathalama.rabotyagaci.build.api.event.BuildCreatedEvent;
import dev.mathalama.rabotyagaci.build.api.event.BuildStepStartedEvent;
import dev.mathalama.rabotyagaci.build.api.event.OrchestratorInternalErrorEvent;
import dev.mathalama.rabotyagaci.build.domain.Build;
import dev.mathalama.rabotyagaci.build.domain.BuildStatus;
import dev.mathalama.rabotyagaci.build.domain.BuildStep;
import dev.mathalama.rabotyagaci.build.domain.StepStatus;
import dev.mathalama.rabotyagaci.build.domain.TriggerType;
import dev.mathalama.rabotyagaci.build.mapper.BuildMapper;
import dev.mathalama.rabotyagaci.build.repository.BuildRepository;
import dev.mathalama.rabotyagaci.build.repository.BuildStepRepository;
import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import dev.mathalama.rabotyagaci.pipeline.api.PipelineService;
import dev.mathalama.rabotyagaci.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.rabotyagaci.pipeline.api.dto.StepDefinition;
import dev.mathalama.rabotyagaci.pipeline.service.impl.GitCloneService;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import dev.mathalama.rabotyagaci.pipeline.api.dto.SecretFileDefinition;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BuildServiceImpl implements BuildService {

    private final BuildRepository buildRepository;
    private final BuildStepRepository buildStepRepository;
    private final ProjectRepository projectRepository;
    private final BuildCacheService buildCacheService;
    private final BuildMapper buildMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final dev.mathalama.rabotyagaci.project.service.impl.SecretCryptoService secretCryptoService;
    private final ObjectMapper objectMapper;
    private final org.springframework.beans.factory.ObjectProvider<BuildOrchestrator> buildOrchestratorProvider;

    @Value("${rabotyagaci.runner.workspace-dir}")
    private String workspaceDirParent;

    @Override
    public BuildResponse trigger(Long projectId, TriggerBuildRequest request, TriggerType type) {
        log.info("Triggering build for project ID: {}, branch: {}, commit: {}", projectId, request.branch(), request.commitSha());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        if (!project.isActive()) {
            throw new BusinessException("Cannot trigger build for inactive project: " + project.getName());
        }

        Build build = Build.builder()
                .project(project)
                .branch(request.branch())
                .commitSha(request.commitSha())
                .authorEmail(request.authorEmail())
                .triggerType(type)
                .status(BuildStatus.PENDING)
                .build();

        Build savedBuild = buildRepository.save(build);
        log.info("Build created with ID: {} in PENDING status", savedBuild.getId());

        // Publish creation event
        eventPublisher.publishEvent(new BuildCreatedEvent(savedBuild.getId(), projectId, type, savedBuild.getCreatedAt()));

        // Start build execution asynchronously via orchestrator
        buildOrchestratorProvider.getObject().executeBuildAsync(
                savedBuild.getId(),
                project.getRepoUrl(),
                savedBuild.getBranch(),
                savedBuild.getCommitSha(),
                project.getPipelineConfigPath()
        );

        return buildMapper.toResponse(savedBuild);
    }

    @Override
    @Transactional
    public void prepareBuildSteps(Long buildId, PipelineDefinition pipelineDef) {
        log.info("Preparing build steps in database for build ID: {}", buildId);
        Build build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Build not found with id " + buildId));

        build.setStatus(BuildStatus.RUNNING);
        build.setStartedAt(Instant.now());

        if (pipelineDef.notifications() != null && pipelineDef.notifications().email() != null) {
            if (pipelineDef.notifications().email().onSuccess() != null) {
                build.setNotifyOnSuccess(String.join(",", pipelineDef.notifications().email().onSuccess()));
            }
            if (pipelineDef.notifications().email().onFailure() != null) {
                build.setNotifyOnFailure(String.join(",", pipelineDef.notifications().email().onFailure()));
            }
        }

        if (pipelineDef.cache() != null && pipelineDef.cache().paths() != null && !pipelineDef.cache().paths().isEmpty()) {
            build.setCachedPaths(String.join(",", pipelineDef.cache().paths()));
            
            // Restore cache
            try {
                Path workspaceDir = Path.of(workspaceDirParent).resolve("build-" + build.getId());
                buildCacheService.restoreCache(build.getProject().getId(), workspaceDir, pipelineDef.cache().paths());
            } catch (Exception e) {
                log.warn("Failed to restore cache for build ID: {}", buildId, e);
            }
        }

        boolean hasExplicitDependsOn = pipelineDef.steps().stream()
                .anyMatch(s -> s.dependsOn() != null && !s.dependsOn().isEmpty());

        List<BuildStep> steps = new ArrayList<>();
        int order = 0;
        String prevStepName = null;

        for (StepDefinition stepDef : pipelineDef.steps()) {
            String secretFilesJson = null;
            if (stepDef.secretFiles() != null && !stepDef.secretFiles().isEmpty()) {
                try {
                    secretFilesJson = objectMapper.writeValueAsString(stepDef.secretFiles());
                } catch (Exception e) {
                    log.error("Failed to serialize secretFiles to JSON for step {}", stepDef.name(), e);
                }
            }

            List<String> deps;
            if (hasExplicitDependsOn) {
                deps = stepDef.dependsOn() != null ? stepDef.dependsOn() : List.of();
            } else {
                deps = prevStepName != null ? List.of(prevStepName) : List.of();
            }
            prevStepName = stepDef.name();

            String dependsOnJson = "[]";
            try {
                dependsOnJson = objectMapper.writeValueAsString(deps);
            } catch (Exception e) {
                log.error("Failed to serialize dependsOn for step {}", stepDef.name(), e);
            }

            BuildStep step = BuildStep.builder()
                    .build(build)
                    .name(stepDef.name())
                    .stepOrder(order++)
                    .dockerImage(stepDef.image())
                    .commands(String.join("\n", stepDef.commands()))
                    .status(StepStatus.PENDING)
                    .privileged(Boolean.TRUE.equals(stepDef.privileged()))
                    .dockerSocket(Boolean.TRUE.equals(stepDef.dockerSocket()))
                    .secretFiles(secretFilesJson)
                    .dependsOn(dependsOnJson)
                    .unresolvedDependenciesCount(deps.size())
                    .build();
            steps.add(step);
        }

        List<BuildStep> savedSteps = buildStepRepository.saveAll(steps);
        build.setSteps(savedSteps);
        buildRepository.save(build);
    }

    @Override
    @Transactional
    public void failBuild(Long buildId, String errorMessage) {
        log.info("Failing build ID: {}. Error: {}", buildId, errorMessage);
        Build build = buildRepository.findById(buildId).orElse(null);
        if (build == null) {
            return;
        }

        Instant finishedAt = Instant.now();
        build.setStatus(BuildStatus.FAILURE);
        build.setFinishedAt(finishedAt);
        buildRepository.save(build);

        // Mark remaining pending/running steps as failed
        if (build.getSteps() != null) {
            for (BuildStep s : build.getSteps()) {
                if (s.getStatus() == StepStatus.PENDING || s.getStatus() == StepStatus.RUNNING) {
                    s.setStatus(StepStatus.FAILURE);
                    s.setFinishedAt(finishedAt);
                    buildStepRepository.save(s);
                }
            }
        }

        eventPublisher.publishEvent(new BuildCompletedEvent(buildId, BuildStatus.FAILURE, finishedAt));
    }

    @Override
    @Transactional(readOnly = true)
    public BuildResponse getById(Long id) {
        Build build = buildRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Build not found with id " + id));
        return buildMapper.toResponse(build);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuildResponse> getAll(Long projectId, Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id " + projectId);
        }
        return buildRepository.findByProjectId(projectId, pageable)
                .map(buildMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuildResponse> getRecentBuilds(int limit) {
        log.debug("Fetching {} recent builds across all projects", limit);
        return buildRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")).stream()
                .limit(limit)
                .map(buildMapper::toResponse)
                .toList();
    }

    @Override
    public void cancel(Long id) {
        log.info("Request to cancel build ID: {}", id);
        Build build = buildRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Build not found with id " + id));

        if (build.getStatus() == BuildStatus.SUCCESS ||
                build.getStatus() == BuildStatus.FAILURE ||
                build.getStatus() == BuildStatus.CANCELLED) {
            log.info("Build {} is already finished, ignore cancel request", id);
            return;
        }

        Instant cancelledAt = Instant.now();
        build.setStatus(BuildStatus.CANCELLED);
        build.setFinishedAt(cancelledAt);
        buildRepository.save(build);

        // Cancel all steps
        for (BuildStep step : build.getSteps()) {
            if (step.getStatus() == StepStatus.PENDING) {
                step.setStatus(StepStatus.SKIPPED);
                buildStepRepository.save(step);
            } else if (step.getStatus() == StepStatus.RUNNING) {
                step.setStatus(StepStatus.FAILURE); // Running steps marked as failure on cancel
                step.setFinishedAt(cancelledAt);
                buildStepRepository.save(step);
            }
        }

        log.info("Build {} has been CANCELLED", id);
        buildStepRepository.saveAll(build.getSteps());
        buildRepository.save(build);

        eventPublisher.publishEvent(new BuildCancelledEvent(id, cancelledAt));
        eventPublisher.publishEvent(new BuildCompletedEvent(id, BuildStatus.CANCELLED, cancelledAt));
    }

    @EventListener
    public void handleOrchestratorInternalError(OrchestratorInternalErrorEvent event) {
        log.error("Received OrchestratorInternalErrorEvent for build ID: {}. Failing build globally.", event.buildId(), event.cause());
        
        Build build = buildRepository.findById(event.buildId()).orElse(null);
        if (build == null || build.getStatus() == BuildStatus.SUCCESS || build.getStatus() == BuildStatus.FAILURE || build.getStatus() == BuildStatus.CANCELLED) {
            return;
        }

        Instant finishedAt = event.occurredAt();
        build.setStatus(BuildStatus.FAILURE);
        build.setFinishedAt(finishedAt);
        buildRepository.save(build);

        // Mark remaining steps as failed
        if (build.getSteps() != null) {
            for (BuildStep s : build.getSteps()) {
                if (s.getStatus() == StepStatus.PENDING || s.getStatus() == StepStatus.RUNNING) {
                    s.setStatus(StepStatus.FAILURE);
                    s.setFinishedAt(finishedAt);
                    buildStepRepository.save(s);
                }
            }
        }

        eventPublisher.publishEvent(new BuildCompletedEvent(build.getId(), BuildStatus.FAILURE, finishedAt));
    }

    @Override
    @Transactional
    public synchronized void onStepCompleted(Long buildId, Long stepId, StepStatus status, int exitCode) {
        log.info("Callback for step completed: build ID {}, step ID {}, status: {}, exitCode: {}",
                buildId, stepId, status, exitCode);

        Build build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Build not found with id " + buildId));

        if (build.getStatus() == BuildStatus.CANCELLED) {
            log.info("Build {} is already cancelled, ignoring step completion callback", buildId);
            return;
        }

        BuildStep completedStep = buildStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Build step not found with id " + stepId));

        Instant finishedAt = Instant.now();
        long durationMs = completedStep.getStartedAt() != null ?
                Duration.between(completedStep.getStartedAt(), finishedAt).toMillis() : 0L;

        completedStep.setStatus(status);
        completedStep.setExitCode(exitCode);
        completedStep.setFinishedAt(finishedAt);
        completedStep.setDurationMs(durationMs);
        buildStepRepository.save(completedStep);

        List<BuildStep> allSteps = buildStepRepository.findByBuildIdOrderByStepOrderAsc(buildId);

        if (status == StepStatus.FAILURE) {
            log.warn("Step '{}' failed for build ID: {}. Skipping downstream dependent steps", completedStep.getName(), buildId);

            Set<String> failedStepNames = new HashSet<>();
            failedStepNames.add(completedStep.getName());

            boolean added;
            do {
                added = false;
                for (BuildStep s : allSteps) {
                    if (s.getStatus() == StepStatus.PENDING && !failedStepNames.contains(s.getName())) {
                        List<String> deps = parseDeps(s.getDependsOn());
                        if (deps.stream().anyMatch(failedStepNames::contains)) {
                            failedStepNames.add(s.getName());
                            s.setStatus(StepStatus.SKIPPED);
                            s.setFinishedAt(finishedAt);
                            buildStepRepository.save(s);
                            added = true;
                        }
                    }
                }
            } while (added);

            boolean anyRunning = allSteps.stream().anyMatch(s -> s.getStatus() == StepStatus.RUNNING);
            if (!anyRunning) {
                log.info("No more running steps for build ID: {}. Finalizing build as FAILURE", buildId);
                build.setStatus(BuildStatus.FAILURE);
                build.setFinishedAt(finishedAt);
                buildRepository.save(build);

                eventPublisher.publishEvent(new BuildCompletedEvent(buildId, BuildStatus.FAILURE, finishedAt));
            }
        } else if (status == StepStatus.SUCCESS) {
            log.info("Step '{}' completed successfully. Decrementing unresolved dependencies for children", completedStep.getName());

            List<String> childStepNames = new ArrayList<>();
            for (BuildStep s : allSteps) {
                if (s.getStatus() == StepStatus.PENDING) {
                    List<String> deps = parseDeps(s.getDependsOn());
                    if (deps.contains(completedStep.getName())) {
                        childStepNames.add(s.getName());
                    }
                }
            }

            if (!childStepNames.isEmpty()) {
                buildStepRepository.decrementUnresolvedDependencies(buildId, childStepNames);
            }

            startNextStep(buildId);
        }
    }

    @Override
    @Transactional
    public synchronized void startNextStep(Long buildId) {
        Build build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Build not found with id " + buildId));

        if (build.getStatus() == BuildStatus.CANCELLED || build.getStatus() == BuildStatus.FAILURE) {
            return;
        }

        try {
            List<BuildStep> allSteps = buildStepRepository.findByBuildIdOrderByStepOrderAsc(buildId);
            List<BuildStep> eligibleSteps = allSteps.stream()
                    .filter(step -> step.getStatus() == StepStatus.PENDING && step.getUnresolvedDependenciesCount() == 0)
                    .toList();

            if (eligibleSteps.isEmpty()) {
                boolean hasRunningSteps = allSteps.stream().anyMatch(step -> step.getStatus() == StepStatus.RUNNING);
                boolean allCompleted = allSteps.stream().allMatch(step -> step.getStatus() == StepStatus.SUCCESS || step.getStatus() == StepStatus.SKIPPED);

                if (!hasRunningSteps) {
                    if (allCompleted) {
                        log.info("All DAG steps completed successfully. Finalizing build ID: {}", build.getId());

                        build.setStatus(BuildStatus.SUCCESS);
                        Instant finishedAt = Instant.now();
                        build.setFinishedAt(finishedAt);
                        buildRepository.save(build);

                        if (build.getCachedPaths() != null && !build.getCachedPaths().isBlank()) {
                            Path workspaceDir = Path.of(workspaceDirParent).resolve("build-" + build.getId());
                            buildCacheService.saveCache(build.getProject().getId(), workspaceDir, List.of(build.getCachedPaths().split(",")));
                        }

                        eventPublisher.publishEvent(new BuildCompletedEvent(build.getId(), BuildStatus.SUCCESS, finishedAt));
                    }
                }
                return;
            }

            Path workspaceDir = Path.of(workspaceDirParent).resolve("build-" + build.getId());

            // Map secrets to environment variables
            java.util.Map<String, String> envVars = new java.util.HashMap<>();
            if (build.getProject().getSecrets() != null) {
                build.getProject().getSecrets().forEach(secret ->
                        envVars.put(secret.getName(), secretCryptoService.decrypt(secret.getValue()))
                );
            }

            for (BuildStep step : eligibleSteps) {
                log.info("Starting eligible DAG build step: '{}' (order {}, ID: {}) for build ID: {}",
                        step.getName(), step.getStepOrder(), step.getId(), buildId);

                step.setStatus(StepStatus.RUNNING);
                step.setStartedAt(Instant.now());
                buildStepRepository.save(step);

                List<String> commands = List.of(step.getCommands().split("\n"));

                // Deserialize secret files mapping
                java.util.Map<String, String> secretFilesMap = new java.util.HashMap<>();
                if (step.getSecretFiles() != null && !step.getSecretFiles().isBlank()) {
                    try {
                        List<SecretFileDefinition> definitions = objectMapper.readValue(
                                step.getSecretFiles(),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, SecretFileDefinition.class)
                        );

                        if (build.getProject().getSecrets() != null) {
                            for (SecretFileDefinition def : definitions) {
                                build.getProject().getSecrets().stream()
                                        .filter(s -> s.getName().equalsIgnoreCase(def.secret()))
                                        .findFirst()
                                        .ifPresent(secret -> {
                                            String decrypted = secretCryptoService.decrypt(secret.getValue());
                                            secretFilesMap.put(def.path(), decrypted);
                                        });
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to deserialize or resolve secretFiles for step ID: {}", step.getId(), e);
                    }
                }

                eventPublisher.publishEvent(new BuildStepStartedEvent(
                        build.getId(),
                        step.getId(),
                        step.getName(),
                        step.getDockerImage(),
                        commands,
                        workspaceDir,
                        envVars,
                        step.isPrivileged(),
                        step.isDockerSocket(),
                        secretFilesMap
                ));
            }
        } catch (Exception e) {
            log.error("Fatal error starting next step for build ID: {}. Failing build.", build.getId(), e);
            failBuild(buildId, e.getMessage());
        }
    }

    private List<String> parseDeps(String dependsOnJson) {
        if (dependsOnJson == null || dependsOnJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    dependsOnJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
