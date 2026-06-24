package dev.mathalama.rabotyagaci.project.service.impl;

import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import dev.mathalama.rabotyagaci.project.api.ProjectService;
import dev.mathalama.rabotyagaci.project.api.dto.CreateProjectRequest;
import dev.mathalama.rabotyagaci.project.api.dto.ProjectResponse;
import dev.mathalama.rabotyagaci.project.api.dto.UpdateProjectRequest;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.mapper.ProjectMapper;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    public ProjectResponse create(CreateProjectRequest request) {
        log.info("Creating new project with name: {}", request.name());

        if (projectRepository.existsByName(request.name())) {
            throw new BusinessException("Project with name '" + request.name() + "' already exists");
        }

        Project project = projectMapper.toEntity(request);
        project.setWebhookSecret(UUID.randomUUID().toString());

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
    public Page<ProjectResponse> getAll(Pageable pageable) {
        log.debug("Fetching all projects with pagination");
        return projectRepository.findAll(pageable)
                .map(projectMapper::toResponse);
    }

    @Override
    public ProjectResponse update(Long id, UpdateProjectRequest request) {
        log.info("Updating project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));

        if (request.name() != null && !request.name().equals(project.getName())
                && projectRepository.existsByName(request.name())) {
            throw new BusinessException("Project with name '" + request.name() + "' already exists");
        }

        projectMapper.updateEntity(project, request);

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
