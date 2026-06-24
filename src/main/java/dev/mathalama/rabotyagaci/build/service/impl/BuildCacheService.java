package dev.mathalama.rabotyagaci.build.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class BuildCacheService {

    @Value("${rabotyagaci.runner.workspace-dir}")
    private String workspaceDirParent;

    public void restoreCache(Long projectId, Path workspaceDir, List<String> paths) {
        if (paths == null || paths.isEmpty()) return;

        Path projectCacheDir = Path.of(workspaceDirParent).resolve("cache").resolve("project-" + projectId);
        if (!Files.exists(projectCacheDir)) {
            log.info("No cache directory found for project ID: {}", projectId);
            return;
        }

        for (String path : paths) {
            Path cachePath = projectCacheDir.resolve(path);
            Path targetPath = workspaceDir.resolve(path);

            if (Files.exists(cachePath)) {
                log.info("Restoring cache for path: {}", path);
                try {
                    // Create parent directories if needed
                    if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    FileSystemUtils.copyRecursively(cachePath, targetPath);
                } catch (IOException e) {
                    log.error("Failed to restore cache from {} to {}", cachePath, targetPath, e);
                }
            } else {
                log.info("Cache path {} does not exist in cache directory", path);
            }
        }
    }

    public void saveCache(Long projectId, Path workspaceDir, List<String> paths) {
        if (paths == null || paths.isEmpty()) return;

        Path projectCacheDir = Path.of(workspaceDirParent).resolve("cache").resolve("project-" + projectId);

        for (String path : paths) {
            Path sourcePath = workspaceDir.resolve(path);
            Path cachePath = projectCacheDir.resolve(path);

            if (Files.exists(sourcePath)) {
                log.info("Saving cache for path: {}", path);
                try {
                    if (cachePath.getParent() != null && !Files.exists(cachePath.getParent())) {
                        Files.createDirectories(cachePath.getParent());
                    }
                    // Overwrite cache with new files
                    if (Files.exists(cachePath)) {
                        FileSystemUtils.deleteRecursively(cachePath);
                    }
                    FileSystemUtils.copyRecursively(sourcePath, cachePath);
                } catch (IOException e) {
                    log.error("Failed to save cache from {} to {}", sourcePath, cachePath, e);
                }
            }
        }
    }
}
