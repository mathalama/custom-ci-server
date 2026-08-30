package dev.mathalama.rabotyagaci.project.api;

import dev.mathalama.rabotyagaci.project.api.dto.CreateProjectRequest;
import dev.mathalama.rabotyagaci.project.api.dto.ProjectResponse;
import dev.mathalama.rabotyagaci.project.api.dto.UpdateProjectRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    ProjectResponse create(CreateProjectRequest request);
    ProjectResponse getById(Long id);
    Page<ProjectResponse> getAll(Pageable pageable);
    ProjectResponse update(Long id, UpdateProjectRequest request);
    void delete(Long id);
}
