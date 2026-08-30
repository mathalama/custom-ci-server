package dev.mathalama.zovik.project.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import dev.mathalama.zovik.common.exception.BusinessException;
import dev.mathalama.zovik.common.exception.ResourceNotFoundException;
import dev.mathalama.zovik.project.api.ProjectService;
import dev.mathalama.zovik.project.api.dto.CreateProjectRequest;
import dev.mathalama.zovik.project.api.dto.ProjectResponse;
import dev.mathalama.zovik.project.api.dto.UpdateProjectRequest;
import dev.mathalama.zovik.project.domain.Project;
import dev.mathalama.zovik.project.mapper.ProjectMapper;
import dev.mathalama.zovik.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final SecretCryptoService secretCryptoService;

    @Override
    public ProjectResponse create(CreateProjectRequest request) {
        log.info("Creating new project with name: {}", request.name());

        if (projectRepository.existsByName(request.name())) {
            throw new BusinessException("Project with name '" + request.name() + "' already exists");
        }

        Project project = projectMapper.toEntity(request);
        project.setWebhookSecret(UUID.randomUUID().toString());

        // Encrypt github token if provided
        if (project.getGithubToken() != null && !project.getGithubToken().isBlank()) {
            project.setGithubToken(secretCryptoService.encrypt(project.getGithubToken()));
        }

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {}", savedProject.getId());

        return projectMapper.toResponse(savedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        log.debug("Fetching project by ID: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "projects", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProjectResponse> getAll(Pageable pageable) {
        log.debug("Fetching all projects with pagination");
        return projectRepository.findAll(pageable)
                .map(projectMapper::toResponse);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectResponse update(Long id, UpdateProjectRequest request) {
        log.info("Updating project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));

        if (request.name() != null && !request.name().equals(project.getName())
                && projectRepository.existsByName(request.name())) {
            throw new BusinessException("Project with name '" + request.name() + "' already exists");
        }

        projectMapper.updateEntity(project, request);

        if (Boolean.TRUE.equals(request.clearGithubToken())) {
            project.setGithubToken(null);
        } else if (request.githubToken() != null) {
            if (request.githubToken().isBlank()) {
                project.setGithubToken(null);
            } else {
                project.setGithubToken(secretCryptoService.encrypt(request.githubToken()));
            }
        }

        Project updatedProject = projectRepository.save(project);
        log.info("Project with ID: {} updated successfully", id);

        return projectMapper.toResponse(updatedProject);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting project with ID: {}", id);
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with id " + id);
        }
        projectRepository.deleteById(id);
        log.info("Project with ID: {} deleted successfully", id);
    }
}
