package dev.mathalama.rabotyagaci.notification.api;

import dev.mathalama.rabotyagaci.build.domain.BuildStatus;

public interface NotificationService {
    void notify(Long buildId, BuildStatus status, String message);
}
