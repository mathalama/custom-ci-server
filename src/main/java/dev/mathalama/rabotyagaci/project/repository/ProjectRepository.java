package dev.mathalama.rabotyagaci.project.repository;

import dev.mathalama.rabotyagaci.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByName(String name);
}
