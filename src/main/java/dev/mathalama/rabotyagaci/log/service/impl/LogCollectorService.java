package dev.mathalama.rabotyagaci.log.service.impl;

import dev.mathalama.rabotyagaci.build.domain.BuildStep;
import dev.mathalama.rabotyagaci.build.repository.BuildStepRepository;
import dev.mathalama.rabotyagaci.log.domain.BuildLog;
import dev.mathalama.rabotyagaci.log.domain.LogStream;
import dev.mathalama.rabotyagaci.log.repository.BuildLogRepository;
import dev.mathalama.rabotyagaci.runner.api.event.LogChunkEvent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogCollectorService {

    private final BuildLogRepository buildLogRepository;
    private final BuildStepRepository buildStepRepository;
    private final ConcurrentLinkedQueue<LogChunkEvent> queue = new ConcurrentLinkedQueue<>();

    @EventListener
    public void handleLogChunk(LogChunkEvent event) {
        queue.add(event);
        if (queue.size() >= 50) {
            flushLogs();
        }
    }

    @Scheduled(fixedDelay = 500)
    public void scheduledFlush() {
        if (!queue.isEmpty()) {
            flushLogs();
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Application shutting down. Flushing remaining logs...");
        flushLogs();
    }

    @Transactional
    public synchronized void flushLogs() {
        if (queue.isEmpty()) {
            return;
        }

        List<LogChunkEvent> chunks = new ArrayList<>();
        LogChunkEvent chunk;
        while ((chunk = queue.poll()) != null) {
            chunks.add(chunk);
        }

        if (chunks.isEmpty()) {
            return;
        }

        log.debug("Flushing {} log chunks to database", chunks.size());
        List<BuildLog> buildLogs = new ArrayList<>();

        for (LogChunkEvent event : chunks) {
            BuildStep step = buildStepRepository.findById(event.buildStepId()).orElse(null);
            if (step == null) {
                log.warn("BuildStep with ID: {} not found. Log chunk discarded.", event.buildStepId());
                continue;
            }

            LogStream stream = "STDERR".equalsIgnoreCase(event.stream()) ? LogStream.STDERR : LogStream.STDOUT;

            BuildLog buildLog = BuildLog.builder()
                    .buildStep(step)
                    .lineNumber(event.lineNumber())
                    .stream(stream)
                    .content(event.content())
                    .timestamp(event.timestamp())
                    .build();
            buildLogs.add(buildLog);
        }

        if (!buildLogs.isEmpty()) {
            buildLogRepository.saveAll(buildLogs);
        }
    }
}
