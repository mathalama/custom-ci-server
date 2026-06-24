package dev.mathalama.rabotyagaci.project.mapper;

import dev.mathalama.rabotyagaci.project.api.dto.CreateProjectRequest;
import dev.mathalama.rabotyagaci.project.api.dto.ProjectResponse;
import dev.mathalama.rabotyagaci.project.api.dto.UpdateProjectRequest;
import dev.mathalama.rabotyagaci.project.domain.Project;
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
    ProjectResponse toResponse(Project entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "webhookSecret", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "isActive", target = "active")
    void updateEntity(@MappingTarget Project entity, UpdateProjectRequest request);
}
