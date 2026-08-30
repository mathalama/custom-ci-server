package dev.mathalama.rabotyagaci.notification.service.impl;

import dev.mathalama.rabotyagaci.build.api.event.BuildCompletedEvent;
import dev.mathalama.rabotyagaci.build.domain.Build;
import dev.mathalama.rabotyagaci.build.domain.BuildStatus;
import dev.mathalama.rabotyagaci.build.repository.BuildRepository;
import dev.mathalama.rabotyagaci.notification.api.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingNotificationService implements NotificationService {

    private final BuildRepository buildRepository;

    @Override
    public void notify(Long buildId, BuildStatus status, String message) {
        log.info("[NOTIFICATION] Build ID: {} finished with status: {}. Message: {}", buildId, status, message);
    }

    @EventListener
    public void handleBuildCompleted(BuildCompletedEvent event) {
        log.info("Notification listener received BuildCompletedEvent for build ID: {}", event.buildId());
        Build build = buildRepository.findById(event.buildId()).orElse(null);
        String projectName = build != null ? build.getProject().getName() : "Unknown Project";

        String message = String.format("Build for project '%s' on branch '%s' completed with status %s",
                projectName,
                build != null ? build.getBranch() : "unknown",
                event.status()
        );

        notify(event.buildId(), event.status(), message);
    }
}
