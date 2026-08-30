package dev.mathalama.zovik.notification.api;

import dev.mathalama.zovik.build.domain.BuildStatus;

public interface NotificationService {
    void notify(Long buildId, BuildStatus status, String message);
}
