package dev.mathalama.zovik.build.service.impl;

import dev.mathalama.zovik.pipeline.api.dto.CacheDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuildCacheServiceTest {

    private BuildCacheService buildCacheService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        buildCacheService = new BuildCacheService();
        ReflectionTestUtils.setField(buildCacheService, "workspaceDirParent", tempDir.toString());
        ReflectionTestUtils.setField(buildCacheService, "maxCacheSizeBytes", 2L * 1024 * 1024 * 1024);
    }

    @Test
    @DisplayName("Should resolve checksum placeholder from target file in workspace")
    void testResolveCacheKey() throws IOException {
        Path workspace = tempDir.resolve("ws");
        Files.createDirectories(workspace);
        Files.writeString(workspace.resolve("package.json"), "{\"name\":\"app\",\"version\":\"1.0.0\"}");

        String keyTemplate = "npm-{{ checksum \"package.json\" }}-v1";
        String resolved = buildCacheService.resolveCacheKey(workspace, keyTemplate);

        assertNotNull(resolved);
        assertTrue(resolved.startsWith("npm-"));
        assertTrue(resolved.endsWith("-v1"));
        assertNotEquals(keyTemplate, resolved);
    }

    @Test
    @DisplayName("Should save and restore cache with Zip compression")
    void testSaveAndRestoreCache_ZipCompression() throws IOException {
        Long projectId = 42L;
        Path workspace = tempDir.resolve("ws-build");
        Files.createDirectories(workspace.resolve("node_modules/my-lib"));
        Files.writeString(workspace.resolve("node_modules/my-lib/index.js"), "module.exports = 'hello';");

        CacheDefinition cacheDef = new CacheDefinition("deps-cache", List.of("node_modules"), List.of());

        // 1. Save cache
        buildCacheService.saveCache(projectId, workspace, cacheDef);

        Path projectCacheDir = tempDir.resolve("cache").resolve("project-" + projectId);
        assertTrue(Files.exists(projectCacheDir.resolve("deps-cache.zip")));

        // 2. Clean workspace
        Files.deleteIfExists(workspace.resolve("node_modules/my-lib/index.js"));
        Files.deleteIfExists(workspace.resolve("node_modules/my-lib"));
        Files.deleteIfExists(workspace.resolve("node_modules"));
        assertFalse(Files.exists(workspace.resolve("node_modules/my-lib/index.js")));

        // 3. Restore cache
        boolean restored = buildCacheService.restoreCache(projectId, workspace, cacheDef);
        assertTrue(restored);
        assertTrue(Files.exists(workspace.resolve("node_modules/my-lib/index.js")));
        assertEquals("module.exports = 'hello';", Files.readString(workspace.resolve("node_modules/my-lib/index.js")));
    }

    @Test
    @DisplayName("Should use restore_keys prefix fallback when exact key is not found")
    void testRestoreKeys_Fallback() throws IOException {
        Long projectId = 99L;
        Path workspace = tempDir.resolve("ws-fallback");
        Files.createDirectories(workspace.resolve(".m2/repository"));
        Files.writeString(workspace.resolve(".m2/repository/artifact.jar"), "dummy jar");

        // Save under generic prefix key
        CacheDefinition initialCache = new CacheDefinition("maven-hash123", List.of(".m2/repository"), List.of());
        buildCacheService.saveCache(projectId, workspace, initialCache);

        // Clear workspace
        Files.deleteIfExists(workspace.resolve(".m2/repository/artifact.jar"));
        assertFalse(Files.exists(workspace.resolve(".m2/repository/artifact.jar")));

        // Request non-existing exact key "maven-hash999" with fallback prefix "maven-"
        CacheDefinition newCache = new CacheDefinition("maven-hash999", List.of(".m2/repository"), List.of("maven-"));
        boolean restored = buildCacheService.restoreCache(projectId, workspace, newCache);

        assertTrue(restored);
        assertTrue(Files.exists(workspace.resolve(".m2/repository/artifact.jar")));
    }

    @Test
    @DisplayName("Should enforce LRU quota and evict oldest archive when max disk size is exceeded")
    void testEnforceProjectQuota_LRUEviction() throws IOException, InterruptedException {
        Long projectId = 100L;
        Path projectCacheDir = tempDir.resolve("cache").resolve("project-" + projectId);
        Files.createDirectories(projectCacheDir);

        // Set small quota: 15 KB
        ReflectionTestUtils.setField(buildCacheService, "maxCacheSizeBytes", 15 * 1024L);

        // Create 3 archive files
        Path file1 = projectCacheDir.resolve("cache1.zip");
        Path file2 = projectCacheDir.resolve("cache2.zip");
        Path file3 = projectCacheDir.resolve("cache3.zip");

        // Write 8 KB to each file (Total 24 KB > 15 KB quota)
        byte[] data = new byte[8 * 1024];
        Files.write(file1, data);
        Thread.sleep(50);
        Files.write(file2, data);
        Thread.sleep(50);
        Files.write(file3, data);

        // Run quota enforcement
        buildCacheService.enforceProjectQuota(projectId);

        // Oldest file (file1) should be evicted
        assertFalse(Files.exists(file1), "Oldest file cache1.zip should have been LRU evicted");
        assertTrue(Files.exists(file3), "Newest file cache3.zip should be preserved");
    }
}
