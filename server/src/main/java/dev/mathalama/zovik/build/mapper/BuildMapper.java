package dev.mathalama.zovik.build.mapper;

import dev.mathalama.zovik.build.api.dto.BuildResponse;
import dev.mathalama.zovik.build.api.dto.BuildStepResponse;
import dev.mathalama.zovik.build.domain.Build;
import dev.mathalama.zovik.build.domain.BuildStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BuildMapper {

    @Mapping(source = "project.id", target = "projectId")
    BuildResponse toResponse(Build entity);

    BuildStepResponse toResponse(BuildStep entity);
}
