package dev.mathalama.zovik.log.controller;

import dev.mathalama.zovik.build.domain.BuildStep;
import dev.mathalama.zovik.log.domain.BuildLog;
import dev.mathalama.zovik.log.domain.LogStream;
import dev.mathalama.zovik.log.repository.BuildLogRepository;
import dev.mathalama.zovik.log.service.impl.LogStreamingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuildLogRepository buildLogRepository;

    @MockitoBean
    private LogStreamingService logStreamingService;

    @Test
    void testGetLogs_ShouldReturnPagedList() throws Exception {
        BuildStep step = BuildStep.builder().id(10L).build();
        BuildLog logLine = BuildLog.builder()
                .id(1L)
                .buildStep(step)
                .lineNumber(1)
                .stream(LogStream.STDOUT)
                .content("echo hello")
                .timestamp(Instant.now())
                .build();

        when(buildLogRepository.findByBuildStepIdOrderByLineNumberAsc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(logLine)));

        mockMvc.perform(get("/api/v1/builds/1/steps/10/logs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].content").value("echo hello"))
                .andExpect(jsonPath("$.data.content[0].lineNumber").value(1));
    }

    @Test
    void testStreamLogs_ShouldOpenSseEmitter() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(logStreamingService.createEmitter(10L)).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/builds/1/steps/10/logs/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }
}
