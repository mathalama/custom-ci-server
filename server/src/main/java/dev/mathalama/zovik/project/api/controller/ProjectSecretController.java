package dev.mathalama.zovik.project.api.controller;

import dev.mathalama.zovik.common.api.dto.ApiResponse;
import dev.mathalama.zovik.project.api.ProjectSecretService;
import dev.mathalama.zovik.project.api.dto.CreateSecretRequest;
import dev.mathalama.zovik.project.api.dto.SecretResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/secrets")
@RequiredArgsConstructor
public class ProjectSecretController {

    private final ProjectSecretService projectSecretService;

    @GetMapping
    public ApiResponse<List<SecretResponse>> getSecrets(@PathVariable Long projectId) {
        return ApiResponse.ok(projectSecretService.getSecrets(projectId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SecretResponse> addSecret(@PathVariable Long projectId, @Valid @RequestBody CreateSecretRequest request) {
        return ApiResponse.ok(projectSecretService.addSecret(projectId, request));
    }

    @DeleteMapping("/{secretId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSecret(@PathVariable Long projectId, @PathVariable Long secretId) {
        projectSecretService.deleteSecret(projectId, secretId);
    }
}
