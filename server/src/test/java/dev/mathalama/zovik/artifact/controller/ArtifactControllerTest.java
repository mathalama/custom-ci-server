package dev.mathalama.zovik.artifact.controller;

import dev.mathalama.zovik.artifact.domain.BuildArtifact;
import dev.mathalama.zovik.artifact.repository.BuildArtifactRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtifactController.class)
class ArtifactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuildArtifactRepository buildArtifactRepository;

    @MockitoBean
    private dev.mathalama.zovik.build.repository.BuildRepository buildRepository;

    @MockitoBean
    private dev.mathalama.zovik.runner.repository.RunnerRepository runnerRepository;

    @Test
    void testGetArtifacts_ShouldReturnList() throws Exception {
        BuildArtifact artifact = BuildArtifact.builder()
                .id(1L)
                .fileName("app.jar")
                .fileSize(1000L)
                .contentType("application/java-archive")
                .createdAt(Instant.now())
                .build();

        when(buildArtifactRepository.findByBuildId(5L)).thenReturn(List.of(artifact));

        mockMvc.perform(get("/api/v1/builds/5/artifacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fileName").value("app.jar"))
                .andExpect(jsonPath("$.data[0].fileSize").value(1000L));
    }
}
