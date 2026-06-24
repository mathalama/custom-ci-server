package dev.mathalama.rabotyagaci.artifact.repository;

import dev.mathalama.rabotyagaci.artifact.domain.BuildArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BuildArtifactRepository extends JpaRepository<BuildArtifact, Long> {
    List<BuildArtifact> findByBuildId(Long buildId);
    List<BuildArtifact> findByCreatedAtBefore(Instant threshold);
}
