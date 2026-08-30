package dev.mathalama.rabotyagaci.project.service.impl;

import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import dev.mathalama.rabotyagaci.project.api.ProjectSecretService;
import dev.mathalama.rabotyagaci.project.api.dto.CreateSecretRequest;
import dev.mathalama.rabotyagaci.project.api.dto.SecretResponse;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.domain.ProjectSecret;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import dev.mathalama.rabotyagaci.project.repository.ProjectSecretRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectSecretServiceImpl implements ProjectSecretService {

    private final ProjectSecretRepository projectSecretRepository;
    private final ProjectRepository projectRepository;
    private final SecretCryptoService secretCryptoService;

    @Override
    @Transactional(readOnly = true)
    public List<SecretResponse> getSecrets(Long projectId) {
        return projectSecretRepository.findByProjectId(projectId).stream()
                .map(secret -> new SecretResponse(secret.getId(), secret.getName(), "******", secret.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public SecretResponse addSecret(Long projectId, CreateSecretRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        if (projectSecretRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new BusinessException("Secret with name '" + request.name() + "' already exists for this project");
        }

        ProjectSecret secret = ProjectSecret.builder()
                .project(project)
                .name(request.name())
                .value(secretCryptoService.encrypt(request.value()))
                .build();

        ProjectSecret savedSecret = projectSecretRepository.save(secret);
        log.info("Secret '{}' added for project ID {}", request.name(), projectId);

        return new SecretResponse(savedSecret.getId(), savedSecret.getName(), "******", savedSecret.getCreatedAt());
    }

    @Override
    public void deleteSecret(Long projectId, Long secretId) {
        ProjectSecret secret = projectSecretRepository.findById(secretId)
                .orElseThrow(() -> new ResourceNotFoundException("Secret not found with id " + secretId));

        if (!secret.getProject().getId().equals(projectId)) {
            throw new BusinessException("Secret does not belong to the specified project");
        }

        projectSecretRepository.delete(secret);
        log.info("Secret ID {} deleted for project ID {}", secretId, projectId);
    }
}
