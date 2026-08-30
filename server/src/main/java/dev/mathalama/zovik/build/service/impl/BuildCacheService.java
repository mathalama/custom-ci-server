package dev.mathalama.zovik.build.service.impl;

import dev.mathalama.zovik.pipeline.api.dto.CacheDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * LRU Dependency Caching Engine.
 * Supports checksum-based key resolution, zip compression/extraction,
 * path traversal protection, and automated disk quota LRU eviction.
 */
@Slf4j
@Service
public class BuildCacheService {

    private static final Pattern CHECKSUM_PATTERN = Pattern.compile("\\{\\{\\s*checksum\\s+\"([^\"]+)\"\\s*\\}\\}");
    private static final long DEFAULT_MAX_CACHE_SIZE_BYTES = 2L * 1024 * 1024 * 1024; // 2 GB per project

    @Value("${zovik.runner.workspace-dir}")
    private String workspaceDirParent;

    @Value("${zovik.cache.max-size-bytes:2147483648}")
    private long maxCacheSizeBytes = DEFAULT_MAX_CACHE_SIZE_BYTES;

    /**
     * Resolves a cache key template by computing checksums for referenced files.
     * Example template: "maven-{{ checksum \"pom.xml\" }}" -> "maven-e3b0c44298fc1c14..."
     */
    public String resolveCacheKey(Path workspaceDir, String keyTemplate) {
        if (keyTemplate == null || keyTemplate.isBlank()) {
            return "default";
        }

        Matcher matcher = CHECKSUM_PATTERN.matcher(keyTemplate);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String relativeFile = matcher.group(1);
            Path targetFile = workspaceDir.resolve(relativeFile).normalize();
            String hash = computeFileHash(targetFile);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(hash));
        }
        matcher.appendTail(sb);
        return sb.toString().trim();
    }

    /**
     * Restores cached paths into workspace from project archive storage.
     */
    public boolean restoreCache(Long projectId, Path workspaceDir, CacheDefinition cacheDef) {
        if (cacheDef == null || cacheDef.paths() == null || cacheDef.paths().isEmpty()) {
            return false;
        }

        Path projectCacheDir = getProjectCacheDir(projectId);
        if (!Files.exists(projectCacheDir)) {
            log.info("No cache repository found for project ID: {}", projectId);
            return false;
        }

        String primaryKey = resolveCacheKey(workspaceDir, cacheDef.key());
        Path archiveFile = projectCacheDir.resolve(sanitizeKey(primaryKey) + ".zip");

        // Try exact match first
        if (!Files.exists(archiveFile) && cacheDef.restoreKeys() != null) {
            for (String prefix : cacheDef.restoreKeys()) {
                Path matched = findBestPrefixMatch(projectCacheDir, sanitizeKey(prefix));
                if (matched != null) {
                    archiveFile = matched;
                    log.info("Found fallback cache matching restore key prefix '{}': {}", prefix, archiveFile.getFileName());
                    break;
                }
            }
        }

        if (!Files.exists(archiveFile)) {
            log.info("No matching cache archive found for key: {} (Project ID: {})", primaryKey, projectId);
            return false;
        }

        log.info("Restoring cache archive '{}' into workspace: {}", archiveFile.getFileName(), workspaceDir);
        try {
            extractZipArchive(archiveFile, workspaceDir);
            // Touch archive for LRU tracking
            Files.setLastModifiedTime(archiveFile, java.nio.file.attribute.FileTime.from(java.time.Instant.now()));
            return true;
        } catch (IOException e) {
            log.error("Failed to extract cache archive {}", archiveFile, e);
            return false;
        }
    }

    /**
     * Legacy adapter for backward compatibility with List<String> paths.
     */
    public void restoreCache(Long projectId, Path workspaceDir, List<String> paths) {
        restoreCache(projectId, workspaceDir, new CacheDefinition(paths));
    }

    /**
     * Saves specified paths from workspace into compressed project cache archive.
     */
    public void saveCache(Long projectId, Path workspaceDir, CacheDefinition cacheDef) {
        if (cacheDef == null || cacheDef.paths() == null || cacheDef.paths().isEmpty()) {
            return;
        }

        Path projectCacheDir = getProjectCacheDir(projectId);
        try {
            if (!Files.exists(projectCacheDir)) {
                Files.createDirectories(projectCacheDir);
            }

            String primaryKey = resolveCacheKey(workspaceDir, cacheDef.key());
            Path archiveFile = projectCacheDir.resolve(sanitizeKey(primaryKey) + ".zip");

            log.info("Archiving cache paths {} to {}", cacheDef.paths(), archiveFile.getFileName());
            createZipArchive(workspaceDir, cacheDef.paths(), archiveFile);

            // Enforce LRU disk quotas after saving
            enforceProjectQuota(projectId);

        } catch (IOException e) {
            log.error("Failed to create cache archive for project ID {}", projectId, e);
        }
    }

    /**
     * Legacy adapter for backward compatibility with List<String> paths.
     */
    public void saveCache(Long projectId, Path workspaceDir, List<String> paths) {
        saveCache(projectId, workspaceDir, new CacheDefinition(paths));
    }

    /**
     * Enforces LRU disk quota for a given project's cache directory.
     * Deletes least recently used (oldest modified) archives when total size exceeds quota.
     */
    public synchronized void enforceProjectQuota(Long projectId) {
        Path projectCacheDir = getProjectCacheDir(projectId);
        if (!Files.exists(projectCacheDir)) return;

        try (Stream<Path> files = Files.list(projectCacheDir)) {
            List<Path> archives = files
                    .filter(p -> p.toString().endsWith(".zip"))
                    .sorted(Comparator.comparingLong(this::getFileLastModified))
                    .toList();

            long totalBytes = 0;
            for (Path archive : archives) {
                totalBytes += Files.size(archive);
            }

            if (totalBytes > maxCacheSizeBytes) {
                log.info("Project ID {} cache total size {} bytes exceeds quota {} bytes. Running LRU eviction.",
                        projectId, totalBytes, maxCacheSizeBytes);

                long targetBytes = (long) (maxCacheSizeBytes * 0.75); // Clean down to 75%
                for (Path oldArchive : archives) {
                    if (totalBytes <= targetBytes) break;
                    long size = Files.size(oldArchive);
                    Files.deleteIfExists(oldArchive);
                    totalBytes -= size;
                    log.info("LRU evicted cache archive: {} (freed {} bytes)", oldArchive.getFileName(), size);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to enforce LRU cache quota for project ID {}", projectId, e);
        }
    }

    /**
     * Background scheduled maintenance task to enforce LRU quotas across all projects.
     */
    @Scheduled(fixedDelay = 3600000) // Every 1 hour
    public void scheduledGlobalQuotaCleanup() {
        Path globalCacheDir = Path.of(workspaceDirParent).resolve("cache");
        if (!Files.exists(globalCacheDir)) return;

        try (Stream<Path> projectDirs = Files.list(globalCacheDir)) {
            projectDirs.filter(Files::isDirectory).forEach(dir -> {
                String dirName = dir.getFileName().toString();
                if (dirName.startsWith("project-")) {
                    try {
                        Long projectId = Long.parseLong(dirName.substring("project-".length()));
                        enforceProjectQuota(projectId);
                    } catch (NumberFormatException ignored) {}
                }
            });
        } catch (IOException e) {
            log.warn("Failed during scheduled global cache quota enforcement", e);
        }
    }

    private void createZipArchive(Path baseDir, List<String> paths, Path zipOutput) throws IOException {
        Path tempZip = zipOutput.getParent().resolve(zipOutput.getFileName().toString() + ".tmp");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tempZip)))) {
            for (String relPathStr : paths) {
                Path source = baseDir.resolve(relPathStr).normalize();
                if (!source.startsWith(baseDir.normalize())) {
                    log.warn("Security warning: Cache source path {} escapes workspace", relPathStr);
                    continue;
                }
                if (!Files.exists(source)) {
                    continue;
                }

                Files.walkFileTree(source, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String zipEntryName = baseDir.relativize(file).toString().replace('\\', '/');
                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        Files.move(tempZip, zipOutput, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void extractZipArchive(Path zipFile, Path targetWorkspaceDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path destFile = targetWorkspaceDir.resolve(entry.getName()).normalize();

                // Zip-Slip Traversal Protection
                if (!destFile.startsWith(targetWorkspaceDir.normalize())) {
                    throw new IOException("Zip-Slip security exception: entry " + entry.getName() + " escapes workspace");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(destFile);
                } else {
                    if (destFile.getParent() != null) {
                        Files.createDirectories(destFile.getParent());
                    }
                    Files.copy(zis, destFile, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private Path findBestPrefixMatch(Path projectCacheDir, String prefix) {
        try (Stream<Path> files = Files.list(projectCacheDir)) {
            return files
                    .filter(p -> p.getFileName().toString().startsWith(prefix) && p.toString().endsWith(".zip"))
                    .max(Comparator.comparingLong(this::getFileLastModified))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private String computeFileHash(Path file) {
        if (!Files.exists(file)) {
            return "empty";
        }
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 16); // 16 chars is optimal for cache keys
        } catch (Exception e) {
            log.warn("Failed to compute hash for file {}", file, e);
            return "hash-err";
        }
    }

    private long getFileLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private Path getProjectCacheDir(Long projectId) {
        return Path.of(workspaceDirParent).resolve("cache").resolve("project-" + projectId);
    }

    private String sanitizeKey(String key) {
        if (key == null) return "default";
        return key.replaceAll("[^a-zA-Z0-9-_.]", "_");
    }
}
