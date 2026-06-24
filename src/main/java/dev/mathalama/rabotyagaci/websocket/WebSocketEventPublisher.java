package dev.mathalama.rabotyagaci.websocket;

import dev.mathalama.rabotyagaci.runner.api.event.BuildStepCompletedEvent;
import dev.mathalama.rabotyagaci.runner.api.event.LogChunkEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleLogChunkEvent(LogChunkEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/builds/" + event.buildId() + "/logs",
                new WsLogPayload(event.buildStepId(), event.stream(), event.content(), event.lineNumber(), event.timestamp().toString())
        );
    }

    @EventListener
    public void handleBuildStepCompletedEvent(BuildStepCompletedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/builds/" + event.buildId() + "/steps",
                new WsStepPayload(event.stepId(), event.status().name(), event.exitCode())
        );
    }

    public record WsLogPayload(Long stepId, String stream, String content, int lineNumber, String timestamp) {}
    public record WsStepPayload(Long stepId, String status, int exitCode) {}
}
