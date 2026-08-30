package dev.mathalama.zovik.build.service.impl;

import dev.mathalama.zovik.build.api.dto.TriggerBuildRequest;
import dev.mathalama.zovik.build.api.dto.BuildResponse;
import dev.mathalama.zovik.build.api.event.BuildCancelledEvent;
import dev.mathalama.zovik.build.api.event.BuildCompletedEvent;
import dev.mathalama.zovik.build.api.event.BuildCreatedEvent;
import dev.mathalama.zovik.build.api.event.BuildStepStartedEvent;
import dev.mathalama.zovik.build.domain.Build;
import dev.mathalama.zovik.build.domain.BuildStatus;
import dev.mathalama.zovik.build.domain.BuildStep;
import dev.mathalama.zovik.build.domain.StepStatus;
import dev.mathalama.zovik.build.domain.TriggerType;
import dev.mathalama.zovik.build.mapper.BuildMapper;
import dev.mathalama.zovik.build.repository.BuildRepository;
import dev.mathalama.zovik.build.repository.BuildStepRepository;
import dev.mathalama.zovik.common.exception.BusinessException;
import dev.mathalama.zovik.pipeline.api.dto.PipelineDefinition;
import dev.mathalama.zovik.pipeline.api.dto.StepDefinition;
import dev.mathalama.zovik.project.domain.Project;
import dev.mathalama.zovik.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildServiceImplTest {

    @Mock
    private BuildRepository buildRepository;
    @Mock
    private BuildStepRepository buildStepRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private BuildMapper buildMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BuildCacheService buildCacheService;
    @Mock
    private dev.mathalama.zovik.project.service.impl.SecretCryptoService secretCryptoService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private org.springframework.beans.factory.ObjectProvider<BuildOrchestrator> buildOrchestratorProvider;
    @Mock
    private BuildOrchestrator buildOrchestrator;

    @InjectMocks
    private BuildServiceImpl buildService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(buildService, "workspaceDirParent", "/tmp/zovik/workspaces");
    }

    @Test
    void trigger_ShouldCreateBuildAndStartAsyncExecution() {
        Long projectId = 1L;
        TriggerBuildRequest request = new TriggerBuildRequest("main", "sha-hash", "author@example.com");
        Project project = Project.builder().id(projectId).name("my-project").repoUrl("https://github.com/my/repo").isActive(true).pipelineConfigPath(".rabotyaga.yaml").build();
        Build build = Build.builder().id(100L).project(project).branch("main").commitSha("sha-hash").triggerType(TriggerType.MANUAL).status(BuildStatus.PENDING).createdAt(Instant.now()).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(buildRepository.save(any(Build.class))).thenReturn(build);
        when(buildOrchestratorProvider.getObject()).thenReturn(buildOrchestrator);

        buildService.trigger(projectId, request, TriggerType.MANUAL);

        verify(buildRepository).save(any(Build.class));
        verify(eventPublisher).publishEvent(any(BuildCreatedEvent.class));
        verify(buildOrchestrator).executeBuildAsync(eq(100L), eq("https://github.com/my/repo"), eq("main"), eq("sha-hash"), eq(".rabotyaga.yaml"));
    }

    @Test
    void trigger_ShouldThrowException_WhenProjectInactive() {
        Long projectId = 1L;
        TriggerBuildRequest request = new TriggerBuildRequest("main", "sha-hash", "author@example.com");
        Project project = Project.builder().id(projectId).name("my-project").isActive(false).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(BusinessException.class, () -> buildService.trigger(projectId, request, TriggerType.MANUAL));
        verify(buildRepository, never()).save(any(Build.class));
    }

    @Test
    void prepareBuildSteps_ShouldSetRunningAndCreateSteps_WhenValid() {
        Long buildId = 100L;
        Project project = Project.builder().id(1L).repoUrl("git-url").pipelineConfigPath(".yaml").secrets(List.of()).build();
        Build build = Build.builder().id(buildId).project(project).branch("main").commitSha("sha-hash").status(BuildStatus.PENDING).build();

        StepDefinition stepDef1 = new StepDefinition("Step1", "alpine", List.of("echo 1"), null);
        StepDefinition stepDef2 = new StepDefinition("Step2", "alpine", List.of("echo 2"), null);
        PipelineDefinition pipelineDef = new PipelineDefinition(1, "my-pipeline", List.of(stepDef1, stepDef2), null, null);

        when(buildRepository.findById(buildId)).thenReturn(Optional.of(build));
        when(buildStepRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BuildStep> list = invocation.getArgument(0);
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setId((long) (i + 1));
            }
            return list;
        });

        buildService.prepareBuildSteps(buildId, pipelineDef);

        assertEquals(BuildStatus.RUNNING, build.getStatus());
        assertEquals(2, build.getSteps().size());
        assertEquals(StepStatus.PENDING, build.getSteps().get(0).getStatus());
        assertEquals(0, build.getSteps().get(0).getUnresolvedDependenciesCount());
        assertEquals(StepStatus.PENDING, build.getSteps().get(1).getStatus());
        assertEquals(1, build.getSteps().get(1).getUnresolvedDependenciesCount());
    }

    @Test
    void failBuild_ShouldSetFailureAndMarkStepsAsFailed() {
        Long buildId = 100L;
        Project project = Project.builder().id(1L).build();
        Build build = Build.builder().id(buildId).project(project).status(BuildStatus.RUNNING).build();
        BuildStep step1 = BuildStep.builder().id(1L).build(build).name("Step1").status(StepStatus.RUNNING).build();
        BuildStep step2 = BuildStep.builder().id(2L).build(build).name("Step2").status(StepStatus.PENDING).build();
        build.setSteps(List.of(step1, step2));

        when(buildRepository.findById(buildId)).thenReturn(Optional.of(build));

        buildService.failBuild(buildId, "Test Error");

        assertEquals(BuildStatus.FAILURE, build.getStatus());
        assertEquals(StepStatus.FAILURE, step1.getStatus());
        assertEquals(StepStatus.FAILURE, step2.getStatus());
        verify(eventPublisher).publishEvent(any(BuildCompletedEvent.class));
    }

    @Test
    @DisplayName("Should decrement child dependencies and start next eligible step when parent succeeds")
    void onStepCompleted_ShouldStartNextStep_WhenPreviousSucceeds() {
        Long buildId = 100L;
        Project project = Project.builder().id(1L).secrets(List.of()).build();
        Build build = Build.builder().id(buildId).project(project).status(BuildStatus.RUNNING).build();
        BuildStep step1 = BuildStep.builder().id(1L).build(build).name("Step1").status(StepStatus.RUNNING).startedAt(Instant.now()).stepOrder(0).commands("echo 1").dependsOn("[]").unresolvedDependenciesCount(0).build();
        BuildStep step2 = BuildStep.builder().id(2L).build(build).name("Step2").status(StepStatus.PENDING).stepOrder(1).commands("echo 2").dependsOn("[\"Step1\"]").unresolvedDependenciesCount(1).build();
        List<BuildStep> allSteps = new ArrayList<>(List.of(step1, step2));
        build.setSteps(allSteps);

        when(buildRepository.findById(buildId)).thenReturn(Optional.of(build));
        when(buildStepRepository.findById(1L)).thenReturn(Optional.of(step1));
        when(buildStepRepository.findByBuildIdOrderByStepOrderAsc(buildId)).thenReturn(allSteps);
        when(buildStepRepository.save(any(BuildStep.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When decrement is called in test, simulate step2 becoming ready
        doAnswer(invocation -> {
            step2.setUnresolvedDependenciesCount(0);
            return 1;
        }).when(buildStepRepository).decrementUnresolvedDependencies(eq(buildId), anyCollection());

        buildService.onStepCompleted(buildId, 1L, StepStatus.SUCCESS, 0);

        assertEquals(StepStatus.SUCCESS, step1.getStatus());
        assertEquals(StepStatus.RUNNING, step2.getStatus());
        verify(eventPublisher).publishEvent(any(BuildStepStartedEvent.class));
    }

    @Test
    void onStepCompleted_ShouldCompleteBuild_WhenLastStepSucceeds() {
        Long buildId = 100L;
        Build build = Build.builder().id(buildId).status(BuildStatus.RUNNING).build();
        BuildStep step1 = BuildStep.builder().id(1L).build(build).name("Step1").status(StepStatus.RUNNING).startedAt(Instant.now()).stepOrder(0).commands("echo 1").dependsOn("[]").unresolvedDependenciesCount(0).build();
        List<BuildStep> allSteps = new ArrayList<>(List.of(step1));
        build.setSteps(allSteps);

        when(buildRepository.findById(buildId)).thenReturn(Optional.of(build));
        when(buildStepRepository.findById(1L)).thenReturn(Optional.of(step1));
        when(buildStepRepository.findByBuildIdOrderByStepOrderAsc(buildId)).thenReturn(allSteps);
        when(buildRepository.save(any(Build.class))).thenAnswer(invocation -> invocation.getArgument(0));

        buildService.onStepCompleted(buildId, 1L, StepStatus.SUCCESS, 0);

        assertEquals(BuildStatus.SUCCESS, build.getStatus());
        verify(eventPublisher).publishEvent(any(BuildCompletedEvent.class));
    }

    @Test
    void onStepCompleted_ShouldFailBuildAndSkipOthers_WhenStepFails() {
        Long buildId = 100L;
        Build build = Build.builder().id(buildId).status(BuildStatus.RUNNING).build();
        BuildStep step1 = BuildStep.builder().id(1L).build(build).name("Step1").status(StepStatus.RUNNING).startedAt(Instant.now()).stepOrder(0).commands("echo 1").dependsOn("[]").unresolvedDependenciesCount(0).build();
        BuildStep step2 = BuildStep.builder().id(2L).build(build).name("Step2").status(StepStatus.PENDING).stepOrder(1).commands("echo 2").dependsOn("[\"Step1\"]").unresolvedDependenciesCount(1).build();
        List<BuildStep> allSteps = new ArrayList<>(List.of(step1, step2));
        build.setSteps(allSteps);

        when(buildRepository.findById(buildId)).thenReturn(Optional.of(build));
        when(buildStepRepository.findById(1L)).thenReturn(Optional.of(step1));
        when(buildStepRepository.findByBuildIdOrderByStepOrderAsc(buildId)).thenReturn(allSteps);
        when(buildRepository.save(any(Build.class))).thenAnswer(invocation -> invocation.getArgument(0));

        buildService.onStepCompleted(buildId, 1L, StepStatus.FAILURE, 1);

        assertEquals(BuildStatus.FAILURE, build.getStatus());
        assertEquals(StepStatus.SKIPPED, step2.getStatus());
        verify(eventPublisher).publishEvent(any(BuildCompletedEvent.class));
    }

    @Test
    void cancel_ShouldCancelBuildAndMarkRunningStepsAsFailure() {
        Long buildId = 100L;
        Build build = Build.builder().id(buildId).status(BuildStatus.RUNNING).build();
        BuildStep step1 = BuildStep.builder().id(1L).build(build).name("Step1").status(StepStatus.RUNNING).stepOrder(0).build();
        BuildStep step2 = BuildStep.builder().id(2L).build(build).name("Step2").status(StepStatus.PENDING).stepOrder(1).build();
        build.setSteps(new ArrayList<>(List.of(step1, step2)));

        when(buildRepository.findById(buildId)).thenReturn(Optional.of(build));
        when(buildRepository.save(any(Build.class))).thenAnswer(invocation -> invocation.getArgument(0));

        buildService.cancel(buildId);

        assertEquals(BuildStatus.CANCELLED, build.getStatus());
        assertEquals(StepStatus.FAILURE, step1.getStatus());
        assertEquals(StepStatus.SKIPPED, step2.getStatus());
        verify(eventPublisher).publishEvent(any(BuildCancelledEvent.class));
        verify(eventPublisher).publishEvent(any(BuildCompletedEvent.class));
    }
}
