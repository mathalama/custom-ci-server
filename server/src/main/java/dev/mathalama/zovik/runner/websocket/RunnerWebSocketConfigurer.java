package dev.mathalama.zovik.runner.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RunnerWebSocketConfigurer implements WebSocketConfigurer {

    private final RunnerWebSocketHandler runnerWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(runnerWebSocketHandler, "/api/v1/runners/ws")
                .setAllowedOrigins("*");
    }
}
