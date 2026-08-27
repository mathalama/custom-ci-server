package dev.mathalama.rabotyagaci.build.api.event;

import java.nio.file.Path;
import java.util.List;

/**
 * Triggered when a build step needs to be executed.
 */
public record BuildStepStartedEvent(
        Long buildId,
        Long stepId,
        String stepName,
        String dockerImage,
        List<String> commands,
        Path workspaceDir,
        java.util.Map<String, String> environmentVariables,
        boolean privileged,
        boolean dockerSocket,
        java.util.Map<String, String> secretFiles
) {}
