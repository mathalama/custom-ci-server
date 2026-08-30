package dev.mathalama.zovik.runner.controller;

import dev.mathalama.zovik.build.api.BuildService;
import dev.mathalama.zovik.build.repository.BuildStepRepository;
import dev.mathalama.zovik.log.service.LogSanitizer;
import dev.mathalama.zovik.runner.api.dto.RunnerRegisterRequest;
import dev.mathalama.zovik.runner.domain.Runner;
import dev.mathalama.zovik.runner.domain.RunnerStatus;
import dev.mathalama.zovik.runner.repository.RunnerRepository;
import dev.mathalama.zovik.runner.service.impl.RunnerJobQueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunnerPullControllerTest {

    @Mock
    private RunnerRepository runnerRepository;
    @Mock
    private RunnerJobQueueService runnerJobQueueService;
    @Mock
    private BuildStepRepository buildStepRepository;
    @Mock
    private BuildService buildService;
    @Mock
    private LogSanitizer logSanitizer;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RunnerPullController controller;

    @Test
    @DisplayName("Should generate one-time registration token with 15m expiration")
    void testGenerateRegistrationToken() {
        when(runnerRepository.save(any(Runner.class))).thenAnswer(inv -> {
            Runner r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        var resp = controller.generateRegistrationToken("Test-Runner");

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().success());
        assertNotNull(resp.getBody().data().get("registrationToken"));
        assertTrue(resp.getBody().data().get("registrationToken").toString().startsWith("rb_reg_"));
    }

    @Test
    @DisplayName("Should exchange valid registration token for permanent hashed token")
    void testRegisterRunner_Success() {
        String regToken = "rb_reg_test123";
        Runner runner = Runner.builder()
                .id(5L)
                .name("Old-Name")
                .registrationToken(regToken)
                .registrationTokenExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .status(RunnerStatus.OFFLINE)
                .build();

        when(runnerRepository.findByRegistrationToken(regToken)).thenReturn(Optional.of(runner));
        when(runnerRepository.save(any(Runner.class))).thenAnswer(inv -> inv.getArgument(0));

        RunnerRegisterRequest request = new RunnerRegisterRequest(regToken, "New-Runner", "linux", 4, 8589934592L);
        var resp = controller.registerRunner(request);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("New-Runner", resp.getBody().data().name());
        assertTrue(resp.getBody().data().runnerToken().startsWith("rb_run_"));
        assertEquals(RunnerStatus.ONLINE, runner.getStatus());
        assertNull(runner.getRegistrationToken());
    }

    @Test
    @DisplayName("Should update lastSeenAt on heartbeat")
    void testHeartbeat_Success() {
        Long runnerId = 1L;
        String rawToken = "my-secret-token";
        String tokenHash = RunnerPullController.hashToken(rawToken);

        Runner runner = Runner.builder()
                .id(runnerId)
                .name("Active-Runner")
                .token(tokenHash)
                .status(RunnerStatus.ONLINE)
                .build();

        when(runnerRepository.findById(runnerId)).thenReturn(Optional.of(runner));
        when(runnerRepository.save(any(Runner.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = controller.heartbeat(runnerId, "Bearer " + rawToken);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(runner.getLastSeenAt());
    }
}
