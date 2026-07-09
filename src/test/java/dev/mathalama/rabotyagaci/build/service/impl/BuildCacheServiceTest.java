package dev.mathalama.rabotyagaci.build.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildCacheServiceTest {

    private BuildCacheService buildCacheService;

    @TempDir
    Path workspaceDirParent;

    @BeforeEach
    void setUp() {
        buildCacheService = new BuildCacheService();
        ReflectionTestUtils.setField(buildCacheService, "workspaceDirParent", workspaceDirParent.toString());
    }

    @Test
    void restoreCache_ValidPath_Success() throws IOException {
        Long projectId = 1L;
        Path projectCacheDir = workspaceDirParent.resolve("cache").resolve("project-" + projectId);
        Files.createDirectories(projectCacheDir.resolve("valid/subdir"));
        Files.writeString(projectCacheDir.resolve("valid/subdir/file.txt"), "cache content");

        Path targetWorkspaceDir = workspaceDirParent.resolve("workspaces").resolve("build-1");
        Files.createDirectories(targetWorkspaceDir);

        buildCacheService.restoreCache(projectId, targetWorkspaceDir, List.of("valid/subdir"));

        assertTrue(Files.exists(targetWorkspaceDir.resolve("valid/subdir/file.txt")));
    }

    @Test
    void restoreCache_PathTraversal_Rejected() throws IOException {
        Long projectId = 1L;
        Path projectCacheDir = workspaceDirParent.resolve("cache").resolve("project-" + projectId);
        Files.createDirectories(projectCacheDir);

        String maliciousPath = "../../outside.txt";
        
        // We write the "malicious" file directly to where it would be resolved from the cache perspective
        Path cachePath = projectCacheDir.resolve(maliciousPath).normalize();
        if (cachePath.getParent() != null) {
            Files.createDirectories(cachePath.getParent());
        }
        Files.writeString(cachePath, "malicious content");

        Path targetWorkspaceDir = workspaceDirParent.resolve("workspaces").resolve("build-1");
        Files.createDirectories(targetWorkspaceDir);

        // Calculate where the target would land, and delete it if it accidentally exists
        Path targetPath = targetWorkspaceDir.resolve(maliciousPath).normalize();
        Files.deleteIfExists(targetPath);

        buildCacheService.restoreCache(projectId, targetWorkspaceDir, List.of(maliciousPath));

        // The file should NOT exist at targetPath because it should have been blocked
        assertFalse(Files.exists(targetPath));
    }
    
    @Test
    void saveCache_ValidPath_Success() throws IOException {
        Long projectId = 1L;
        Path targetWorkspaceDir = workspaceDirParent.resolve("workspaces").resolve("build-1");
        Files.createDirectories(targetWorkspaceDir.resolve("target/classes"));
        Files.writeString(targetWorkspaceDir.resolve("target/classes/MyClass.class"), "bytecode");

        buildCacheService.saveCache(projectId, targetWorkspaceDir, List.of("target/classes"));

        Path projectCacheDir = workspaceDirParent.resolve("cache").resolve("project-" + projectId);
        assertTrue(Files.exists(projectCacheDir.resolve("target/classes/MyClass.class")));
    }
    
    @Test
    void saveCache_PathTraversal_Rejected() throws IOException {
        Long projectId = 1L;
        Path targetWorkspaceDir = workspaceDirParent.resolve("workspaces").resolve("build-1");
        Files.createDirectories(targetWorkspaceDir);
        
        String maliciousPath = "../../../etc/cron.d/evil";
        
        // Try to save something from outside the workspace into the cache (or vice versa)
        Path sourcePath = targetWorkspaceDir.resolve(maliciousPath).normalize();
        if (sourcePath.getParent() != null) {
            Files.createDirectories(sourcePath.getParent());
        }
        Files.writeString(sourcePath, "evil payload");

        Path projectCacheDir = workspaceDirParent.resolve("cache").resolve("project-" + projectId);
        Path cachePath = projectCacheDir.resolve(maliciousPath).normalize();
        Files.deleteIfExists(cachePath);

        buildCacheService.saveCache(projectId, targetWorkspaceDir, List.of(maliciousPath));

        // The file should NOT have been copied to the cache because it was rejected by path traversal protection
        assertFalse(Files.exists(cachePath));
    }
}
