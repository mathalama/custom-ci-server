package dev.mathalama.rabotyagaci.log.service.impl;

import dev.mathalama.rabotyagaci.build.domain.BuildStep;
import dev.mathalama.rabotyagaci.build.repository.BuildStepRepository;
import dev.mathalama.rabotyagaci.log.domain.BuildLog;
import dev.mathalama.rabotyagaci.log.repository.BuildLogRepository;
import dev.mathalama.rabotyagaci.runner.api.event.LogChunkEvent;
import dev.mathalama.rabotyagaci.log.service.LogSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogCollectorServiceTest {

    @Mock
    private BuildLogRepository buildLogRepository;

    @Mock
    private BuildStepRepository buildStepRepository;

    @Spy
    private LogSanitizer logSanitizer = new LogSanitizer();

    @InjectMocks
    private LogCollectorService logCollectorService;

    private BuildStep buildStep;

    @BeforeEach
    void setUp() {
        buildStep = BuildStep.builder().id(10L).name("Build Step").build();
    }

    @Test
    void testFlushLogs_WhenThresholdReached() {
        when(buildStepRepository.findById(10L)).thenReturn(Optional.of(buildStep));

        for (int i = 1; i <= 50; i++) {
            logCollectorService.handleLogChunk(new LogChunkEvent(1L, 10L, "STDOUT", "Log " + i, i, Instant.now()));
        }

        ArgumentCaptor<List<BuildLog>> logCaptor = ArgumentCaptor.forClass(List.class);
        verify(buildLogRepository, times(1)).saveAll(logCaptor.capture());

        List<BuildLog> savedLogs = logCaptor.getValue();
        assertEquals(50, savedLogs.size());
        assertEquals("Log 1", savedLogs.get(0).getContent());
        assertEquals(1, savedLogs.get(0).getLineNumber());
    }

    @Test
    void testScheduledFlush() {
        when(buildStepRepository.findById(10L)).thenReturn(Optional.of(buildStep));

        logCollectorService.handleLogChunk(new LogChunkEvent(1L, 10L, "STDOUT", "Log single", 1, Instant.now()));

        verify(buildLogRepository, never()).saveAll(anyList());

        logCollectorService.scheduledFlush();

        verify(buildLogRepository, times(1)).saveAll(anyList());
    }
}
