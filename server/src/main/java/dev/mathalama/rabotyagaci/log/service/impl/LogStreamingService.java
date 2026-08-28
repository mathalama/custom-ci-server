package dev.mathalama.rabotyagaci.log.service.impl;

import dev.mathalama.rabotyagaci.log.domain.BuildLog;
import dev.mathalama.rabotyagaci.log.repository.BuildLogRepository;
import dev.mathalama.rabotyagaci.log.service.LogSanitizer;
import dev.mathalama.rabotyagaci.runner.api.event.LogChunkEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogStreamingService {

    private final BuildLogRepository buildLogRepository;
    private final LogSanitizer logSanitizer;
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long stepId) {
        log.info("Creating SSE emitter for step ID: {}", stepId);
        // Timeout 10 minutes
        SseEmitter emitter = new SseEmitter(600_000L);

        Set<SseEmitter> stepEmitters = emitters.computeIfAbsent(stepId, k -> new CopyOnWriteArraySet<>());
        stepEmitters.add(emitter);

        emitter.onCompletion(() -> stepEmitters.remove(emitter));
        emitter.onTimeout(() -> stepEmitters.remove(emitter));
        emitter.onError((ex) -> stepEmitters.remove(emitter));

        // Send history first
        List<BuildLog> history = buildLogRepository.findByBuildStepIdOrderByLineNumberAsc(stepId);
        for (BuildLog logLine : history) {
            try {
                String sanitized = logSanitizer.sanitizeLine(stepId, logLine.getContent());
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(logLine.getLineNumber()))
                        .name("log-chunk")
                        .data(new SseLogPayload(logLine.getStream().name(), sanitized, logLine.getLineNumber(), logLine.getTimestamp().toString())));
            } catch (IOException e) {
                log.warn("Failed to send historical log chunk to emitter: {}", e.getMessage());
                emitter.completeWithError(e);
                stepEmitters.remove(emitter);
                break;
            }
        }

        return emitter;
    }

    @EventListener
    public void handleLogChunkEvent(LogChunkEvent event) {
        Set<SseEmitter> stepEmitters = emitters.get(event.buildStepId());
        if (stepEmitters == null || stepEmitters.isEmpty()) {
            return;
        }

        log.debug("Streaming log chunk to {} emitters for step ID: {}", stepEmitters.size(), event.buildStepId());
        SseLogPayload payload = new SseLogPayload(
                event.stream(),
                event.content(),
                event.lineNumber(),
                event.timestamp().toString()
        );

        for (SseEmitter emitter : stepEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.lineNumber()))
                        .name("log-chunk")
                        .data(payload));
            } catch (IOException e) {
                log.debug("Emitter closed or failed, removing: {}", e.getMessage());
                stepEmitters.remove(emitter);
            }
        }
    }

    public record SseLogPayload(String stream, String content, int lineNumber, String timestamp) {}
}
