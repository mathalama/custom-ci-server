package dev.mathalama.rabotyagaci.project.service.impl;

import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import dev.mathalama.rabotyagaci.project.api.dto.CreateProjectRequest;
import dev.mathalama.rabotyagaci.project.api.dto.ProjectResponse;
import dev.mathalama.rabotyagaci.project.api.dto.UpdateProjectRequest;
import dev.mathalama.rabotyagaci.project.domain.GitProvider;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.mapper.ProjectMapper;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private SecretCryptoService secretCryptoService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void create_ShouldCreateProject_WhenNameIsUnique() {
        CreateProjectRequest request = new CreateProjectRequest("test-project", "https://github.com/test/repo", GitProvider.GITHUB, "main", ".rabotyaga.yaml", null);
        Project project = Project.builder()
                .name("test-project")
                .repoUrl("https://github.com/test/repo")
                .gitProvider(GitProvider.GITHUB)
                .defaultBranch("main")
                .pipelineConfigPath(".rabotyaga.yaml")
                .build();
        Project savedProject = Project.builder()
                .id(1L)
                .name("test-project")
                .repoUrl("https://github.com/test/repo")
                .gitProvider(GitProvider.GITHUB)
                .defaultBranch("main")
                .webhookSecret("random-uuid-string")
                .pipelineConfigPath(".rabotyaga.yaml")
                .isActive(true)
                .build();
        ProjectResponse expectedResponse = new ProjectResponse(1L, "test-project", "https://github.com/test/repo", GitProvider.GITHUB, "main", "random-uuid-string", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectRepository.existsByName(request.name())).thenReturn(false);
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(projectMapper.toResponse(savedProject)).thenReturn(expectedResponse);

        ProjectResponse response = projectService.create(request);

        assertNotNull(response);
        assertEquals(expectedResponse.id(), response.id());
        assertEquals(expectedResponse.name(), response.name());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void create_ShouldThrowBusinessException_WhenNameExists() {
        CreateProjectRequest request = new CreateProjectRequest("test-project", "https://github.com/test/repo", GitProvider.GITHUB, "main", ".rabotyaga.yaml", null);
        when(projectRepository.existsByName(request.name())).thenReturn(true);

        assertThrows(BusinessException.class, () -> projectService.create(request));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void getById_ShouldReturnResponse_WhenProjectExists() {
        Long id = 1L;
        Project project = new Project();
        ProjectResponse expectedResponse = new ProjectResponse(1L, "test-project", "https://github.com/test/repo", GitProvider.GITHUB, "main", "secret", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(expectedResponse);

        ProjectResponse response = projectService.getById(id);

        assertNotNull(response);
        assertEquals(expectedResponse.id(), response.id());
    }

    @Test
    void getById_ShouldThrowResourceNotFoundException_WhenProjectDoesNotExist() {
        Long id = 1L;
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getById(id));
    }

    @Test
    void getAll_ShouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Project project = new Project();
        Page<Project> page = new PageImpl<>(List.of(project));
        ProjectResponse expectedResponse = new ProjectResponse(1L, "test-project", "https://github.com/test/repo", GitProvider.GITHUB, "main", "secret", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectRepository.findAll(pageable)).thenReturn(page);
        when(projectMapper.toResponse(project)).thenReturn(expectedResponse);

        Page<ProjectResponse> result = projectService.getAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedResponse.id(), result.getContent().get(0).id());
    }

    @Test
    void update_ShouldUpdateProject_WhenValid() {
        Long id = 1L;
        UpdateProjectRequest request = new UpdateProjectRequest("new-name", null, null, null, null, null, null, null);
        Project project = Project.builder()
                .id(id)
                .name("old-name")
                .build();
        Project savedProject = Project.builder()
                .id(id)
                .name("new-name")
                .build();
        ProjectResponse expectedResponse = new ProjectResponse(id, "new-name", "https://github.com/test/repo", GitProvider.GITHUB, "main", "secret", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectRepository.existsByName("new-name")).thenReturn(false);
        when(projectRepository.save(project)).thenReturn(savedProject);
        when(projectMapper.toResponse(savedProject)).thenReturn(expectedResponse);

        ProjectResponse response = projectService.update(id, request);

        assertNotNull(response);
        assertEquals("new-name", response.name());
        verify(projectMapper).updateEntity(project, request);
    }

    @Test
    void update_ShouldClearGithubToken_WhenClearFlagIsTrue() {
        Long id = 1L;
        UpdateProjectRequest request = new UpdateProjectRequest(null, null, null, null, null, null, null, true);
        Project project = Project.builder()
                .id(id)
                .githubToken("encrypted-token")
                .build();
        Project savedProject = Project.builder()
                .id(id)
                .githubToken(null)
                .build();
        ProjectResponse expectedResponse = new ProjectResponse(id, "old-name", "https://github.com/test/repo", GitProvider.GITHUB, "main", null, ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(savedProject);
        when(projectMapper.toResponse(savedProject)).thenReturn(expectedResponse);

        ProjectResponse response = projectService.update(id, request);

        assertNotNull(response);
        assertNull(project.getGithubToken());
        verify(projectMapper).updateEntity(project, request);
        verifyNoInteractions(secretCryptoService);
    }

    @Test
    void update_ShouldEncryptGithubToken_WhenNewTokenProvided() {
        Long id = 1L;
        UpdateProjectRequest request = new UpdateProjectRequest(null, null, null, null, null, null, "plain-token", false);
        Project project = Project.builder()
                .id(id)
                .githubToken(null)
                .build();
        Project savedProject = Project.builder()
                .id(id)
                .githubToken("encrypted-token")
                .build();
        ProjectResponse expectedResponse = new ProjectResponse(id, "old-name", "https://github.com/test/repo", GitProvider.GITHUB, "main", "********", ".rabotyaga.yaml", true, null, Instant.now(), Instant.now());

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(secretCryptoService.encrypt("plain-token")).thenReturn("encrypted-token");
        when(projectRepository.save(project)).thenReturn(savedProject);
        when(projectMapper.toResponse(savedProject)).thenReturn(expectedResponse);

        ProjectResponse response = projectService.update(id, request);

        assertNotNull(response);
        assertEquals("encrypted-token", project.getGithubToken());
        verify(secretCryptoService).encrypt("plain-token");
    }

    @Test
    void update_ShouldThrowBusinessException_WhenNewNameExists() {
        Long id = 1L;
        UpdateProjectRequest request = new UpdateProjectRequest("existing-name", null, null, null, null, null, null, null);
        Project project = Project.builder()
                .id(id)
                .name("old-name")
                .build();

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectRepository.existsByName("existing-name")).thenReturn(true);

        assertThrows(BusinessException.class, () -> projectService.update(id, request));
    }

    @Test
    void delete_ShouldDelete_WhenProjectExists() {
        Long id = 1L;
        when(projectRepository.existsById(id)).thenReturn(true);

        projectService.delete(id);

        verify(projectRepository).deleteById(id);
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenProjectDoesNotExist() {
        Long id = 1L;
        when(projectRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> projectService.delete(id));
        verify(projectRepository, never()).deleteById(id);
    }
}
