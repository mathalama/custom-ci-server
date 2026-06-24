package dev.mathalama.rabotyagaci.build.repository;

import dev.mathalama.rabotyagaci.build.domain.BuildStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildStepRepository extends JpaRepository<BuildStep, Long> {
    List<BuildStep> findByBuildIdOrderByStepOrderAsc(Long buildId);
}
