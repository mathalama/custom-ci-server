package dev.mathalama.rabotyagaci.webhook.service.impl;

import dev.mathalama.rabotyagaci.build.api.BuildService;
import dev.mathalama.rabotyagaci.build.api.dto.TriggerBuildRequest;
import dev.mathalama.rabotyagaci.build.domain.TriggerType;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import dev.mathalama.rabotyagaci.webhook.api.event.WebhookReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventListener {

    private final BuildService buildService;
    private final ProjectRepository projectRepository;

    @Async
    @EventListener
    public void handleWebhookReceived(WebhookReceivedEvent event) {
        log.info("Received WebhookReceivedEvent for project ID: {}, branch: {}", event.projectId(), event.branch());

        Project project = projectRepository.findById(event.projectId()).orElse(null);
        if (project == null) {
            log.error("Project with ID: {} not found for webhook event", event.projectId());
            return;
        }

        if (!project.isActive()) {
            log.warn("Project {} is inactive, skipping auto-trigger", project.getName());
            return;
        }

        // Check if branch matches default branch
        if (!project.getDefaultBranch().equals(event.branch())) {
            log.info("Webhook branch '{}' does not match project default branch '{}'. Skipping auto-trigger.",
                    event.branch(), project.getDefaultBranch());
            return;
        }

        log.info("Auto-triggering build for project {} on branch {}", project.getName(), event.branch());
        TriggerBuildRequest triggerRequest = new TriggerBuildRequest(event.branch(), event.commitSha(), event.authorEmail());
        buildService.trigger(project.getId(), triggerRequest, TriggerType.WEBHOOK);
    }
}
