package dev.mathalama.zovik.build.controller;

import dev.mathalama.zovik.common.exception.ResourceNotFoundException;
import dev.mathalama.zovik.build.api.BuildService;
import dev.mathalama.zovik.build.api.dto.BuildResponse;
import dev.mathalama.zovik.build.api.dto.TriggerBuildRequest;
import dev.mathalama.zovik.build.domain.BuildStatus;
import dev.mathalama.zovik.build.domain.TriggerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuildController.class)
class BuildControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BuildService buildService;

    @Test
    void trigger_ShouldReturn201_WhenValidRequest() throws Exception {
        Long projectId = 1L;
        TriggerBuildRequest request = new TriggerBuildRequest("main", "sha-hash", "author@example.com");
        BuildResponse response = new BuildResponse(100L, projectId, "sha-hash", "main", TriggerType.MANUAL, BuildStatus.PENDING, null, null, Instant.now(), List.of());

        when(buildService.trigger(eq(projectId), any(TriggerBuildRequest.class), eq(TriggerType.MANUAL))).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects/{projectId}/trigger", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void trigger_ShouldReturn400_WhenInvalidRequest() throws Exception {
        Long projectId = 1L;
        TriggerBuildRequest request = new TriggerBuildRequest("", "", "author@example.com");

        mockMvc.perform(post("/api/v1/projects/{projectId}/trigger", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getById_ShouldReturnBuild_WhenExists() throws Exception {
        Long id = 100L;
        BuildResponse response = new BuildResponse(id, 1L, "sha-hash", "main", TriggerType.MANUAL, BuildStatus.SUCCESS, Instant.now(), Instant.now(), Instant.now(), List.of());

        when(buildService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/builds/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void getById_ShouldReturn404_WhenDoesNotExist() throws Exception {
        Long id = 999L;
        when(buildService.getById(id)).thenThrow(new ResourceNotFoundException("Build not found"));

        mockMvc.perform(get("/api/v1/builds/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getAll_ShouldReturnPagedResponse() throws Exception {
        Long projectId = 1L;
        BuildResponse response = new BuildResponse(100L, projectId, "sha-hash", "main", TriggerType.MANUAL, BuildStatus.SUCCESS, Instant.now(), Instant.now(), Instant.now(), List.of());
        
        when(buildService.getAll(eq(projectId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/builds")
                        .param("projectId", projectId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(100L));
    }

    @Test
    void cancel_ShouldReturn200() throws Exception {
        Long id = 100L;

        mockMvc.perform(post("/api/v1/builds/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
