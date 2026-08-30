package dev.mathalama.zovik.runner.websocket;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import dev.mathalama.zovik.build.api.BuildService;
import dev.mathalama.zovik.build.domain.StepStatus;
import dev.mathalama.zovik.runner.api.event.LogChunkEvent;
import dev.mathalama.zovik.runner.domain.Runner;
import dev.mathalama.zovik.runner.domain.RunnerStatus;
import dev.mathalama.zovik.runner.repository.RunnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunnerWebSocketHandler extends TextWebSocketHandler {

    private final RunnerRepository runnerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final org.springframework.beans.factory.ObjectProvider<BuildService> buildServiceProvider;

    private final Map<Long, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToRunnerId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String query = uri.getQuery();
        String token = null;
        if (query != null && query.contains("token=")) {
            token = query.split("token=")[1].split("&")[0];
        }

        if (token == null) {
            log.warn("Runner tried to connect without token. Closing connection.");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Optional<Runner> runnerOpt = runnerRepository.findByToken(token);
        if (runnerOpt.isEmpty()) {
            log.warn("Invalid runner token: {}. Closing connection.", token);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Runner runner = runnerOpt.get();
        runner.setStatus(RunnerStatus.ONLINE);
        runnerRepository.save(runner);

        activeSessions.put(runner.getId(), session);
        sessionToRunnerId.put(session.getId(), runner.getId());

        log.info("Runner '{}' connected successfully. Host: {}", runner.getName(), runner.getHost());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long runnerId = sessionToRunnerId.get(session.getId());
        if (runnerId == null) {
            return;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), new TypeReference<Map<String, Object>>() {});
            String type = (String) payload.get("type");

            if ("INIT".equalsIgnoreCase(type)) {
                String os = (String) payload.get("os");
                Integer cpuCores = (Integer) payload.get("cpuCores");
                Long memoryBytes = null;
                if (payload.get("memoryBytes") != null) {
                    memoryBytes = Long.valueOf(payload.get("memoryBytes").toString());
                }

                Runner runner = runnerRepository.findById(runnerId).orElse(null);
                if (runner != null) {
                    runner.setOs(os);
                    runner.setCpuCores(cpuCores);
                    runner.setMemoryBytes(memoryBytes);
                    runnerRepository.save(runner);
                }
            } else if ("LOG".equalsIgnoreCase(type)) {
                Long buildId = Long.valueOf(payload.get("buildId").toString());
                Long stepId = Long.valueOf(payload.get("stepId").toString());
                String content = (String) payload.get("content");

                // Publish log chunk event to master log collector
                eventPublisher.publishEvent(new LogChunkEvent(buildId, stepId, "STDOUT", content, 1, java.time.Instant.now()));
            } else if ("COMPLETED".equalsIgnoreCase(type)) {
                Long buildId = Long.valueOf(payload.get("buildId").toString());
                Long stepId = Long.valueOf(payload.get("stepId").toString());
                int exitCode = (int) payload.get("exitCode");

                log.info("Runner step execution completed. Step: {}, exitCode: {}", stepId, exitCode);

                // Set runner status back to ONLINE (free)
                updateRunnerStatus(runnerId, RunnerStatus.ONLINE);

                // Notify BuildService of completion
                StepStatus status = (exitCode == 0) ? StepStatus.SUCCESS : StepStatus.FAILURE;
                buildServiceProvider.getObject().onStepCompleted(buildId, stepId, status, exitCode);
            } else if ("ERROR".equalsIgnoreCase(type)) {
                Long buildId = Long.valueOf(payload.get("buildId").toString());
                Long stepId = Long.valueOf(payload.get("stepId").toString());
                String content = (String) payload.get("content");

                log.error("Runner step execution failed. Step: {}, error: {}", stepId, content);

                // Stream the error log to interface
                eventPublisher.publishEvent(new LogChunkEvent(buildId, stepId, "STDERR", "Runner execution error: " + content + "\n", 1, java.time.Instant.now()));

                updateRunnerStatus(runnerId, RunnerStatus.ONLINE);

                buildServiceProvider.getObject().onStepCompleted(buildId, stepId, StepStatus.FAILURE, 1);
            }
        } catch (Exception e) {
            log.error("Error handling message from runner: {}", runnerId, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long runnerId = sessionToRunnerId.remove(session.getId());
        if (runnerId != null) {
            activeSessions.remove(runnerId);
            updateRunnerStatus(runnerId, RunnerStatus.OFFLINE);
            log.warn("Runner ID '{}' connection closed. Status: {}", runnerId, status);
        }
    }

    private void updateRunnerStatus(Long runnerId, RunnerStatus status) {
        Runner runner = runnerRepository.findById(runnerId).orElse(null);
        if (runner != null) {
            runner.setStatus(status);
            runnerRepository.save(runner);
        }
    }

    public boolean hasRunner(Long runnerId) {
        WebSocketSession session = activeSessions.get(runnerId);
        return session != null && session.isOpen();
    }

    public void executeTask(Long runnerId, Map<String, Object> taskPayload) throws IOException {
        WebSocketSession session = activeSessions.get(runnerId);
        if (session == null || !session.isOpen()) {
            throw new IOException("Runner " + runnerId + " is not connected");
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(taskPayload)));
        updateRunnerStatus(runnerId, RunnerStatus.BUSY);
    }
}
