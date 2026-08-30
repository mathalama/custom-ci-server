package dev.mathalama.zovik.log.controller;

import dev.mathalama.zovik.common.api.dto.ApiResponse;
import dev.mathalama.zovik.log.domain.BuildLog;
import dev.mathalama.zovik.log.repository.BuildLogRepository;
import dev.mathalama.zovik.log.service.impl.LogStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/builds/{buildId}/steps/{stepId}/logs")
@RequiredArgsConstructor
public class LogController {

    private final BuildLogRepository buildLogRepository;
    private final LogStreamingService logStreamingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BuildLogDto>>> getLogs(
            @PathVariable Long buildId,
            @PathVariable Long stepId,
            Pageable pageable
    ) {
        log.info("Fetching logs for step ID: {}, page: {}", stepId, pageable.getPageNumber());
        Page<BuildLogDto> logs = buildLogRepository.findByBuildStepIdOrderByLineNumberAsc(stepId, pageable)
                .map(logLine -> new BuildLogDto(
                        logLine.getId(),
                        logLine.getLineNumber(),
                        logLine.getStream().name(),
                        logLine.getContent(),
                        logLine.getTimestamp()
                ));

        return ResponseEntity.ok(new ApiResponse<>(true, logs, null, Instant.now()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(
            @PathVariable Long buildId,
            @PathVariable Long stepId
    ) {
        log.info("Opening logs stream for build ID: {}, step ID: {}", buildId, stepId);
        return logStreamingService.createEmitter(stepId);
    }

    public record BuildLogDto(Long id, int lineNumber, String stream, String content, Instant timestamp) {}
}
