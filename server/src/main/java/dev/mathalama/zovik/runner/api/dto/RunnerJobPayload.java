package dev.mathalama.zovik.runner.api.dto;

import java.util.List;
import java.util.Map;

public record RunnerJobPayload(
        Long buildId,
        Long stepId,
        String stepName,
        String dockerImage,
        List<String> commands,
        Map<String, String> environmentVariables,
        boolean privileged,
        boolean dockerSocket,
        Map<String, String> secretFiles,
        String repoUrl,
        String commitRef,
        String gitToken
) {}
