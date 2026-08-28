package dev.mathalama.rabotyagaci.runner.mapper;

import dev.mathalama.rabotyagaci.runner.api.dto.RunnerResponse;
import dev.mathalama.rabotyagaci.runner.domain.Runner;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RunnerMapper {
    RunnerResponse toResponse(Runner runner);
    List<RunnerResponse> toResponseList(List<Runner> runners);
}
