package dev.mathalama.zovik.project.mapper;

import dev.mathalama.zovik.project.api.dto.CreateProjectRequest;
import dev.mathalama.zovik.project.api.dto.ProjectResponse;
import dev.mathalama.zovik.project.api.dto.UpdateProjectRequest;
import dev.mathalama.zovik.project.domain.Project;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProjectMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "webhookSecret", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toEntity(CreateProjectRequest request);

    @Mapping(source = "active", target = "isActive")
    @Mapping(target = "githubToken", expression = "java(entity.getGithubToken() != null && !entity.getGithubToken().isBlank() ? \"********\" : null)")
    ProjectResponse toResponse(Project entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "webhookSecret", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "isActive", target = "active")
    void updateEntity(@MappingTarget Project entity, UpdateProjectRequest request);
}
