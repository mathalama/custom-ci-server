package dev.mathalama.rabotyagaci.runner.controller;

import dev.mathalama.rabotyagaci.build.api.BuildService;
import dev.mathalama.rabotyagaci.build.domain.BuildStep;
import dev.mathalama.rabotyagaci.build.repository.BuildStepRepository;
import dev.mathalama.rabotyagaci.common.api.dto.ApiResponse;
import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import dev.mathalama.rabotyagaci.log.service.LogSanitizer;
import dev.mathalama.rabotyagaci.runner.api.dto.*;
import dev.mathalama.rabotyagaci.runner.api.event.LogChunkEvent;
import dev.mathalama.rabotyagaci.runner.domain.Runner;
import dev.mathalama.rabotyagaci.runner.domain.RunnerStatus;
import dev.mathalama.rabotyagaci.runner.repository.RunnerRepository;
import dev.mathalama.rabotyagaci.runner.service.impl.RunnerJobQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/runners")
@RequiredArgsConstructor
public class RunnerPullController {

    private final RunnerRepository runnerRepository;
    private final RunnerJobQueueService runnerJobQueueService;
    private final BuildStepRepository buildStepRepository;
    private final BuildService buildService;
    private final LogSanitizer logSanitizer;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 1. Generate one-time registration token (valid for 15 minutes).
     */
    @PostMapping("/tokens/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateRegistrationToken(
            @RequestParam(defaultValue = "Remote-Runner") String name
    ) {
        String regToken = "rb_reg_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        Runner runner = Runner.builder()
                .name(name)
                .status(RunnerStatus.OFFLINE)
                .token(hashToken(UUID.randomUUID().toString())) // Temporary placeholder token
                .registrationToken(regToken)
                .registrationTokenExpiresAt(expiresAt)
                .build();

        Runner saved = runnerRepository.save(runner);
        log.info("Generated registration token for runner ID {}: {} (expires at {})", saved.getId(), regToken, expiresAt);

        Map<String, Object> data = Map.of(
                "runnerId", saved.getId(),
                "registrationToken", regToken,
                "expiresAt", expiresAt.toString()
        );

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * 2. Runner exchanges registration token for permanent runner token.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RunnerRegisterResponse>> registerRunner(
            @RequestBody RunnerRegisterRequest request
    ) {
        log.info("Runner registration attempt with token: {}", request.registrationToken());

        Runner runner = runnerRepository.findByRegistrationToken(request.registrationToken())
                .orElseThrow(() -> new BusinessException("Invalid or non-existent registration token"));

        if (runner.getRegistrationTokenExpiresAt() != null && runner.getRegistrationTokenExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Registration token has expired. Please generate a new one.");
        }

        String permanentToken = "rb_run_" + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(permanentToken);

        if (request.name() != null && !request.name().isBlank()) {
            runner.setName(request.name());
        }
        runner.setToken(tokenHash);
        runner.setRegistrationToken(null);
        runner.setRegistrationTokenExpiresAt(null);
        runner.setStatus(RunnerStatus.ONLINE);
        runner.setLastSeenAt(Instant.now());
        runner.setOs(request.os());
        runner.setCpuCores(request.cpuCores());
        runner.setMemoryBytes(request.memoryBytes());

        runnerRepository.save(runner);
        log.info("Runner '{}' (ID: {}) registered successfully", runner.getName(), runner.getId());

        RunnerRegisterResponse response = new RunnerRegisterResponse(runner.getId(), permanentToken, runner.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 3. Long Polling for next job.
     */
    @GetMapping("/{id}/jobs/next")
    public DeferredResult<ResponseEntity<RunnerJobPayload>> pollNextJob(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        validateRunnerAuth(id, authHeader);
        return runnerJobQueueService.pollNextJob(id);
    }

    /**
     * 4. Heartbeat endpoint.
     */
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Runner runner = validateRunnerAuth(id, authHeader);
        runner.setLastSeenAt(Instant.now());
        if (runner.getStatus() == RunnerStatus.OFFLINE) {
            runner.setStatus(RunnerStatus.ONLINE);
        }
        runnerRepository.save(runner);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 5. Stream log chunks from remote runner.
     */
    @PostMapping("/{id}/jobs/{stepId}/logs")
    public ResponseEntity<ApiResponse<Void>> streamLogs(
            @PathVariable Long id,
            @PathVariable Long stepId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RunnerLogsRequest request
    ) {
        validateRunnerAuth(id, authHeader);

        BuildStep step = buildStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Build step not found with id " + stepId));

        if (request.lines() != null) {
            int lineNum = request.fromLine();
            String stream = (request.stream() != null) ? request.stream() : "STDOUT";

            for (String line : request.lines()) {
                String sanitized = logSanitizer.sanitizeLine(stepId, line);
                eventPublisher.publishEvent(new LogChunkEvent(
                        step.getBuild().getId(),
                        stepId,
                        stream,
                        sanitized,
                        lineNum++,
                        Instant.now()
                ));
            }
        }

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 6. Complete job execution report.
     */
    @PostMapping("/{id}/jobs/{stepId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeJob(
            @PathVariable Long id,
            @PathVariable Long stepId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RunnerCompleteJobRequest request
    ) {
        validateRunnerAuth(id, authHeader);

        BuildStep step = buildStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Build step not found with id " + stepId));

        log.info("Runner ID {} completed step '{}' (ID: {}) with status: {}, exit code: {}",
                id, step.getName(), stepId, request.status(), request.exitCode());

        buildService.onStepCompleted(step.getBuild().getId(), stepId, request.status(), request.exitCode());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    private Runner validateRunnerAuth(Long runnerId, String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("Missing or invalid Authorization header");
        }

        String rawToken = authHeader.substring("Bearer ".length()).trim();
        String tokenHash = hashToken(rawToken);

        Runner runner = runnerRepository.findById(runnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Runner not found with id " + runnerId));

        if (!runner.getToken().equals(tokenHash) && !runner.getToken().equals(rawToken)) {
            throw new BusinessException("Unauthorized runner token");
        }

        return runner;
    }

    public static String hashToken(String rawToken) {
        if (rawToken == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return rawToken;
        }
    }
}
