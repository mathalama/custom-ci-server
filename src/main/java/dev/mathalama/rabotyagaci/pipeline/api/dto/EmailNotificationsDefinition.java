package dev.mathalama.rabotyagaci.pipeline.api.dto;

import java.util.List;

public record EmailNotificationsDefinition(
        List<String> onSuccess,
        List<String> onFailure
) {}
