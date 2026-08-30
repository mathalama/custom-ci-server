package dev.mathalama.zovik.runner.service.impl;

import dev.mathalama.zovik.build.api.BuildService;
import dev.mathalama.zovik.build.domain.BuildStep;
import dev.mathalama.zovik.build.domain.StepStatus;
import dev.mathalama.zovik.build.repository.BuildStepRepository;
import dev.mathalama.zovik.runner.domain.Runner;
import dev.mathalama.zovik.runner.domain.RunnerStatus;
import dev.mathalama.zovik.runner.repository.RunnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerHeartbeatMonitor {

    private final RunnerRepository runnerRepository;
    private final BuildStepRepository buildStepRepository;
    private final BuildService buildService;

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void checkRunnerHeartbeats() {
        Instant cutoff = Instant.now().minusSeconds(40);
        List<Runner> timedOutRunners = runnerRepository.findByStatusNotAndLastSeenAtBefore(RunnerStatus.OFFLINE, cutoff);

        for (Runner runner : timedOutRunners) {
            log.warn("Runner '{}' (ID: {}) timed out (last seen at {}). Marking as OFFLINE",
                    runner.getName(), runner.getId(), runner.getLastSeenAt());

            runner.setStatus(RunnerStatus.OFFLINE);
            runnerRepository.save(runner);

            // Fail any in-flight steps running on this disconnected runner
            List<BuildStep> runningSteps = buildStepRepository.findAll().stream()
                    .filter(s -> s.getRunner() != null && s.getRunner().getId().equals(runner.getId()) && s.getStatus() == StepStatus.RUNNING)
                    .toList();

            for (BuildStep step : runningSteps) {
                log.error("Failing orphan build step '{}' (ID: {}) because runner '{}' went offline",
                        step.getName(), step.getId(), runner.getName());
                try {
                    buildService.onStepCompleted(step.getBuild().getId(), step.getId(), StepStatus.FAILURE, 137);
                } catch (Exception e) {
                    log.error("Failed to mark orphan step as failed", e);
                }
            }
        }
    }
}
