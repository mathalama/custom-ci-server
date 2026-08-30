package dev.mathalama.rabotyagaci.artifact.controller;

import dev.mathalama.rabotyagaci.artifact.domain.BuildArtifact;
import dev.mathalama.rabotyagaci.artifact.repository.BuildArtifactRepository;
import dev.mathalama.rabotyagaci.common.api.dto.ApiResponse;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/builds/{buildId}/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final BuildArtifactRepository buildArtifactRepository;
    private final dev.mathalama.rabotyagaci.build.repository.BuildRepository buildRepository;
    private final dev.mathalama.rabotyagaci.runner.repository.RunnerRepository runnerRepository;

    @org.springframework.beans.factory.annotation.Value("${rabotyagaci.artifacts.storage-dir}")
    private String storageDirParent;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadArtifact(
            @PathVariable Long buildId,
            @RequestParam String filename,
            @RequestParam String stepName,
            @RequestHeader("X-Runner-Token") String token,
            @RequestBody byte[] content
    ) throws IOException {
        log.info("Received artifact upload request for build ID: {}, filename: {}, stepName: {}", buildId, filename, stepName);

        Optional<dev.mathalama.rabotyagaci.runner.domain.Runner> runnerOpt = runnerRepository.findByToken(token);
        if (runnerOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid runner token");
        }

        dev.mathalama.rabotyagaci.build.domain.Build build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Build not found with ID: " + buildId));

        Path storageDir = Path.of(storageDirParent).resolve("build-" + buildId);
        Files.createDirectories(storageDir);

        Path destFile = storageDir.resolve(filename);
        Files.write(destFile, content);

        String contentType = Files.probeContentType(destFile);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        BuildArtifact artifact = BuildArtifact.builder()
                .build(build)
                .fileName(filename)
                .filePath(destFile.toAbsolutePath().toString())
                .fileSize(content.length)
                .contentType(contentType)
                .build();

        buildArtifactRepository.save(artifact);
        log.info("Successfully saved uploaded artifact: {} (size: {} bytes)", filename, content.length);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BuildArtifactDto>>> getArtifacts(@PathVariable Long buildId) {
        log.info("Fetching list of artifacts for build ID: {}", buildId);
        List<BuildArtifactDto> artifacts = buildArtifactRepository.findByBuildId(buildId).stream()
                .map(a -> new BuildArtifactDto(
                        a.getId(),
                        a.getFileName(),
                        a.getFileSize(),
                        a.getContentType(),
                        a.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(true, artifacts, null, Instant.now()));
    }

    @GetMapping("/{artifactId}/download")
    public ResponseEntity<Resource> downloadArtifact(
            @PathVariable Long buildId,
            @PathVariable Long artifactId
    ) throws IOException {
        log.info("Downloading artifact ID: {} for build ID: {}", artifactId, buildId);

        BuildArtifact artifact = buildArtifactRepository.findById(artifactId)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found with id " + artifactId));

        if (!artifact.getBuild().getId().equals(buildId)) {
            throw new ResourceNotFoundException("Artifact does not belong to this build");
        }

        Path path = Path.of(artifact.getFilePath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Artifact file does not exist on disk");
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(path.toFile()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + artifact.getFileName() + "\"")
                .contentLength(artifact.getFileSize())
                .contentType(MediaType.parseMediaType(artifact.getContentType()))
                .body(resource);
    }

    public record BuildArtifactDto(Long id, String fileName, long fileSize, String contentType, Instant createdAt) {}
}
