package dev.mathalama.rabotyagaci.build.controller;

import dev.mathalama.rabotyagaci.common.api.dto.ApiResponse;
import dev.mathalama.rabotyagaci.common.api.dto.PagedResponse;
import dev.mathalama.rabotyagaci.build.api.BuildService;
import dev.mathalama.rabotyagaci.build.api.dto.BuildResponse;
import dev.mathalama.rabotyagaci.build.api.dto.TriggerBuildRequest;
import dev.mathalama.rabotyagaci.build.domain.TriggerType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BuildController {

    private final BuildService buildService;

    @PostMapping("/api/v1/projects/{projectId}/trigger")
    public ResponseEntity<ApiResponse<BuildResponse>> trigger(
            @PathVariable Long projectId,
            @Valid @RequestBody TriggerBuildRequest request) {
        BuildResponse response = buildService.trigger(projectId, request, TriggerType.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/builds/{id}")
    public ResponseEntity<ApiResponse<BuildResponse>> getById(@PathVariable Long id) {
        BuildResponse response = buildService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/builds")
    public ResponseEntity<ApiResponse<PagedResponse<BuildResponse>>> getAll(
            @RequestParam Long projectId,
            @PageableDefault(size = 10) Pageable pageable) {
        PagedResponse<BuildResponse> response = PagedResponse.from(buildService.getAll(projectId, pageable));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/v1/builds/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        buildService.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
