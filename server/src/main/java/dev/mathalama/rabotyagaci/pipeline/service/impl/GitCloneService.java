package dev.mathalama.rabotyagaci.pipeline.service.impl;

import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import dev.mathalama.rabotyagaci.project.service.impl.GitHubIntegrationService;
import dev.mathalama.rabotyagaci.project.service.impl.SecretCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitCloneService {

    private final ProjectRepository projectRepository;
    private final GitHubIntegrationService gitHubIntegrationService;
    private final SecretCryptoService secretCryptoService;

    /**
     * Clones repository if it does not exist, or fetches updates if it does.
     */
    public void cloneOrPull(String repoUrl, Path targetDir) {
        String token = resolveTokenForRepo(repoUrl);
        UsernamePasswordCredentialsProvider credentialsProvider = null;
        if (token != null && !token.isBlank()) {
            credentialsProvider = new UsernamePasswordCredentialsProvider(token, "");
        }

        if (Files.exists(targetDir)) {
            try {
                try (Git git = Git.open(targetDir.toFile())) {
                    log.info("Fetching updates for repository: {}", repoUrl);
                    var fetchCommand = git.fetch();
                    if (credentialsProvider != null) {
                        fetchCommand.setCredentialsProvider(credentialsProvider);
                    }
                    fetchCommand.call();
                }
                return;
            } catch (Exception e) {
                log.warn("Failed to open existing git repo at {}, deleting and re-cloning. Error: {}", targetDir, e.getMessage());
                deleteDirectory(targetDir);
            }
        }

        log.info("Cloning repository: {} into {}", repoUrl, targetDir);
        try {
            Files.createDirectories(targetDir);
            var cloneCommand = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(targetDir.toFile())
                    .setCloneAllBranches(true);
            if (credentialsProvider != null) {
                cloneCommand.setCredentialsProvider(credentialsProvider);
            }
            try (Git git = cloneCommand.call()) {
                log.info("Repository cloned successfully: {}", repoUrl);
            }
        } catch (Exception e) {
            throw new BusinessException("Failed to clone repository: " + repoUrl + ". Error: " + e.getMessage(), e);
        }
    }

    private String resolveTokenForRepo(String repoUrl) {
        if (repoUrl == null) return null;
        
        // 1. Try to find a project with this repo URL
        List<Project> projects = projectRepository.findAll();
        for (Project p : projects) {
            if (repoUrl.equalsIgnoreCase(p.getRepoUrl())) {
                String pToken = p.getGithubToken();
                if (pToken != null && !pToken.isBlank()) {
                    try {
                        return secretCryptoService.decrypt(pToken);
                    } catch (Exception e) {
                        log.error("Failed to decrypt token for project {}", p.getName(), e);
                    }
                }
            }
        }
        
        // 2. Fall back to global GitHub token if it is a GitHub repo
        if (repoUrl.toLowerCase().contains("github.com")) {
            return gitHubIntegrationService.getToken();
        }
        
        return null;
    }

    /**
     * Checks out a specific commit hash, branch or tag.
     */
    public void checkoutCommit(Path repoDir, String commitSha) {
        log.info("Checking out commit/branch '{}' in {}", commitSha, repoDir);
        try (Git git = Git.open(repoDir.toFile())) {
            git.checkout()
                    .setName(commitSha)
                    .setForceRefUpdate(true)
                    .call();
            log.info("Successfully checked out '{}'", commitSha);
        } catch (Exception e) {
            throw new BusinessException("Failed to checkout commit: " + commitSha + ". Error: " + e.getMessage(), e);
        }
    }

    /**
     * Reads a file from the repository working directory.
     */
    public String readFile(Path repoDir, String filePath) {
        Path file = repoDir.resolve(filePath);
        log.debug("Reading file: {}", file);

        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("Configuration file not found in repository: " + filePath);
        }

        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new BusinessException("Failed to read file from repository: " + filePath, e);
        }
    }

    private void deleteDirectory(Path path) {
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        } catch (IOException e) {
            log.error("Failed to delete directory recursively: {}", path, e);
        }
    }
}
