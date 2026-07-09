package dev.mathalama.rabotyagaci.notification.service.impl;

import dev.mathalama.rabotyagaci.build.api.event.BuildCompletedEvent;
import dev.mathalama.rabotyagaci.build.api.event.BuildCreatedEvent;
import dev.mathalama.rabotyagaci.build.domain.Build;
import dev.mathalama.rabotyagaci.build.domain.BuildStatus;
import dev.mathalama.rabotyagaci.build.repository.BuildRepository;
import dev.mathalama.rabotyagaci.project.domain.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class GitHubStatusEventListener {

    private final BuildRepository buildRepository;
    private final GitHubStatusService gitHubStatusService;

    @Value("${rabotyagaci.frontend.url:http://localhost:3001}")
    private String frontendUrl;

    @Async
    @EventListener
    public void handleBuildCreated(BuildCreatedEvent event) {
        processStatus(event.buildId(), "pending", "Build started");
    }

    @Async
    @EventListener
    public void handleBuildCompleted(BuildCompletedEvent event) {
        String status = "error";
        String description = "Build failed";
        
        if (event.status() == BuildStatus.SUCCESS) {
            status = "success";
            description = "Build succeeded";
        } else if (event.status() == BuildStatus.FAILURE) {
            status = "failure";
        } else if (event.status() == BuildStatus.CANCELLED) {
            status = "error";
            description = "Build cancelled";
        }
        
        processStatus(event.buildId(), status, description);
    }

    @Transactional
    protected void processStatus(Long buildId, String githubState, String description) {
        Build build = buildRepository.findById(buildId).orElse(null);
        if (build == null || build.getCommitSha() == null) {
            return;
        }

        Project project = build.getProject();
        String token = project.getGithubToken();
        
        if (token == null || token.isBlank()) {
            return; // No token configured for this project
        }

        String targetUrl = frontendUrl + "/builds/" + buildId;
        
        gitHubStatusService.updateCommitStatus(
                project.getRepoUrl(),
                build.getCommitSha(),
                githubState,
                description,
                targetUrl,
                token
        );
    }
}
