package dev.mathalama.zovik.project.controller;

import tools.jackson.databind.ObjectMapper;
import dev.mathalama.zovik.common.exception.ResourceNotFoundException;
import dev.mathalama.zovik.project.api.ProjectService;
import dev.mathalama.zovik.project.api.dto.CreateProjectRequest;
import dev.mathalama.zovik.project.api.dto.ProjectResponse;
import dev.mathalama.zovik.project.api.dto.UpdateProjectRequest;
import dev.mathalama.zovik.project.domain.GitProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void create_ShouldReturn201_WhenValidRequest() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("my-app", "https://github.com/my/repo", GitProvider.GITHUB, "main", ".rabotyaga.yaml", null);
        ProjectResponse response = new ProjectResponse(1L, "my-app", "https://github.com/my/repo", GitProvider.GITHUB, "main", "secret", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectService.create(any(CreateProjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("my-app"));
    }

    @Test
    void create_ShouldReturn400_WhenInvalidRequest() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("", "https://github.com/my/repo", GitProvider.GITHUB, "main", ".rabotyaga.yaml", null);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation failed: Project name cannot be blank"));
    }

    @Test
    void getById_ShouldReturnProject_WhenExists() throws Exception {
        Long id = 1L;
        ProjectResponse response = new ProjectResponse(id, "my-app", "https://github.com/my/repo", GitProvider.GITHUB, "main", "secret", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.name").value("my-app"));
    }

    @Test
    void getById_ShouldReturn404_WhenDoesNotExist() throws Exception {
        Long id = 99L;
        when(projectService.getById(id)).thenThrow(new ResourceNotFoundException("Project not found with id " + id));

        mockMvc.perform(get("/api/v1/projects/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Project not found with id " + id));
    }

    @Test
    void getAll_ShouldReturnPagedResponse() throws Exception {
        ProjectResponse response = new ProjectResponse(1L, "my-app", "https://github.com/my/repo", GitProvider.GITHUB, "main", "secret", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());
        when(projectService.getAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/projects")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void update_ShouldReturnUpdatedProject_WhenValid() throws Exception {
        Long id = 1L;
        UpdateProjectRequest request = new UpdateProjectRequest("new-name", null, null, null, null, null, null, null);
        ProjectResponse response = new ProjectResponse(id, "new-name", "https://github.com/my/repo", GitProvider.GITHUB, "main", "secret", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectService.update(eq(id), any(UpdateProjectRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/projects/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("new-name"));
    }

    @Test
    void delete_ShouldReturn204_WhenExists() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/api/v1/projects/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_ShouldReturn404_WhenDoesNotExist() throws Exception {
        Long id = 99L;
        doThrow(new ResourceNotFoundException("Project not found with id " + id)).when(projectService).delete(id);

        mockMvc.perform(delete("/api/v1/projects/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Project not found with id " + id));
    }
}
