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

@Slf4j
@RestController
@RequestMapping("/api/v1/builds/{buildId}/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final BuildArtifactRepository buildArtifactRepository;

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
