package dev.mathalama.rabotyagaci.webhook.service.impl;

import dev.mathalama.rabotyagaci.build.api.BuildService;
import dev.mathalama.rabotyagaci.build.api.dto.TriggerBuildRequest;
import dev.mathalama.rabotyagaci.build.domain.TriggerType;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import dev.mathalama.rabotyagaci.webhook.api.event.WebhookReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookEventListenerTest {

    @Mock
    private BuildService buildService;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private WebhookEventListener webhookEventListener;

    private Project testProject;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id(1L)
                .name("test-project")
                .defaultBranch("main")
                .isActive(true)
                .build();
    }

    @Test
    void testHandleWebhookReceivedSuccess() {
        WebhookReceivedEvent event = new WebhookReceivedEvent(
                1L,
                "main",
                "6113728f27ae82c3b1e177c8d37d402b743f6351",
                "commit-msg",
                Instant.now()
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        webhookEventListener.handleWebhookReceived(event);

        verify(buildService, times(1)).trigger(
                eq(1L),
                refEq(new TriggerBuildRequest("main", "6113728f27ae82c3b1e177c8d37d402b743f6351")),
                eq(TriggerType.WEBHOOK)
        );
    }

    @Test
    void testHandleWebhookReceivedBranchMismatch() {
        WebhookReceivedEvent event = new WebhookReceivedEvent(
                1L,
                "feature-branch",
                "6113728f27ae82c3b1e177c8d37d402b743f6351",
                "commit-msg",
                Instant.now()
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        webhookEventListener.handleWebhookReceived(event);

        verify(buildService, never()).trigger(any(), any(), any());
    }

    @Test
    void testHandleWebhookReceivedInactiveProject() {
        testProject.setActive(false);
        WebhookReceivedEvent event = new WebhookReceivedEvent(
                1L,
                "main",
                "6113728f27ae82c3b1e177c8d37d402b743f6351",
                "commit-msg",
                Instant.now()
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        webhookEventListener.handleWebhookReceived(event);

        verify(buildService, never()).trigger(any(), any(), any());
    }

    @Test
    void testHandleWebhookReceivedProjectNotFound() {
        WebhookReceivedEvent event = new WebhookReceivedEvent(
                999L,
                "main",
                "6113728f27ae82c3b1e177c8d37d402b743f6351",
                "commit-msg",
                Instant.now()
        );

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        webhookEventListener.handleWebhookReceived(event);

        verify(buildService, never()).trigger(any(), any(), any());
    }
}
