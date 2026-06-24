package dev.mathalama.rabotyagaci.project.repository;

import dev.mathalama.rabotyagaci.project.domain.ProjectSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectSecretRepository extends JpaRepository<ProjectSecret, Long> {
    List<ProjectSecret> findByProjectId(Long projectId);
    Optional<ProjectSecret> findByProjectIdAndName(Long projectId, String name);
    boolean existsByProjectIdAndName(Long projectId, String name);
}
