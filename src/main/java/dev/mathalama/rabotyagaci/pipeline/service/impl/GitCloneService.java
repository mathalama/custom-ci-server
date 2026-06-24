package dev.mathalama.rabotyagaci.pipeline.service.impl;

import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Slf4j
@Service
public class GitCloneService {

    /**
     * Clones repository if it does not exist, or fetches updates if it does.
     */
    public void cloneOrPull(String repoUrl, Path targetDir) {
        if (Files.exists(targetDir)) {
            try {
                try (Git git = Git.open(targetDir.toFile())) {
                    log.info("Fetching updates for repository: {}", repoUrl);
                    git.fetch().call();
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
            try (Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(targetDir.toFile())
                    .setCloneAllBranches(true)
                    .call()) {
                log.info("Repository cloned successfully: {}", repoUrl);
            }
        } catch (Exception e) {
            throw new BusinessException("Failed to clone repository: " + repoUrl, e);
        }
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
