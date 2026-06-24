package dev.mathalama.rabotyagaci.build.listener;

import dev.mathalama.rabotyagaci.build.api.BuildService;
import dev.mathalama.rabotyagaci.runner.api.event.BuildStepCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BuildStepCompletedListener {

    private final BuildService buildService;

    @EventListener
    public void handleBuildStepCompleted(BuildStepCompletedEvent event) {
        log.info("Received BuildStepCompletedEvent for build ID: {}, step ID: {}, status: {}",
                event.buildId(), event.stepId(), event.status());
        buildService.onStepCompleted(event.buildId(), event.stepId(), event.status(), event.exitCode());
    }
}
