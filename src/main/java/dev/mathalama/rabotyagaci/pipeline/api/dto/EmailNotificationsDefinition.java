package dev.mathalama.rabotyagaci.pipeline.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmailNotificationsDefinition(
        @JsonProperty("on_success") List<String> onSuccess,
        @JsonProperty("on_failure") List<String> onFailure
) {}
