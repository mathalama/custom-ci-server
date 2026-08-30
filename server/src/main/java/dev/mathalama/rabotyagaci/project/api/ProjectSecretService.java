package dev.mathalama.rabotyagaci.project.api;

import dev.mathalama.rabotyagaci.project.api.dto.CreateSecretRequest;
import dev.mathalama.rabotyagaci.project.api.dto.SecretResponse;

import java.util.List;

public interface ProjectSecretService {
    List<SecretResponse> getSecrets(Long projectId);
    SecretResponse addSecret(Long projectId, CreateSecretRequest request);
    void deleteSecret(Long projectId, Long secretId);
}
