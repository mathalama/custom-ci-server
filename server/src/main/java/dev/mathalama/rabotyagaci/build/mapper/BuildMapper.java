package dev.mathalama.rabotyagaci.build.mapper;

import dev.mathalama.rabotyagaci.build.api.dto.BuildResponse;
import dev.mathalama.rabotyagaci.build.api.dto.BuildStepResponse;
import dev.mathalama.rabotyagaci.build.domain.Build;
import dev.mathalama.rabotyagaci.build.domain.BuildStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BuildMapper {

    @Mapping(source = "project.id", target = "projectId")
    BuildResponse toResponse(Build entity);

    BuildStepResponse toResponse(BuildStep entity);
}
