package dev.mathalama.zovik.runner.controller;

import dev.mathalama.zovik.common.api.dto.ApiResponse;
import dev.mathalama.zovik.runner.api.dto.CreateRunnerRequest;
import dev.mathalama.zovik.runner.api.dto.RunnerResponse;
import dev.mathalama.zovik.runner.domain.Runner;
import dev.mathalama.zovik.runner.domain.RunnerStatus;
import dev.mathalama.zovik.runner.mapper.RunnerMapper;
import dev.mathalama.zovik.runner.repository.RunnerRepository;
import dev.mathalama.zovik.runner.service.impl.RunnerProvisionerService;
import dev.mathalama.zovik.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/runners")
@RequiredArgsConstructor
public class RunnerController {

    private final RunnerRepository runnerRepository;
    private final RunnerProvisionerService runnerProvisionerService;
    private final RunnerMapper runnerMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<RunnerResponse>> create(@Valid @RequestBody CreateRunnerRequest request) {
        Runner runner = Runner.builder()
                .name(request.name())
                .host(request.host())
                .port(request.port())
                .username(request.username())
                .password(request.password())
                .sshKey(request.sshKey())
                .status(RunnerStatus.PROVISIONING)
                .token(UUID.randomUUID().toString())
                .build();

        Runner saved = runnerRepository.save(runner);
        runnerProvisionerService.provisionRunnerAsync(saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(runnerMapper.toResponse(saved)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RunnerResponse>>> getAll() {
        List<Runner> runners = runnerRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(runnerMapper.toResponseList(runners)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RunnerResponse>> getById(@PathVariable Long id) {
        Runner runner = runnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Runner not found with ID: " + id));
        return ResponseEntity.ok(ApiResponse.ok(runnerMapper.toResponse(runner)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Runner runner = runnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Runner not found with ID: " + id));
        runnerRepository.delete(runner);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
