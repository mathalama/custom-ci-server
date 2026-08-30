package dev.mathalama.zovik.build.repository;

import dev.mathalama.zovik.build.domain.BuildStep;
import dev.mathalama.zovik.build.domain.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BuildStepRepository extends JpaRepository<BuildStep, Long> {

    List<BuildStep> findByBuildIdOrderByStepOrderAsc(Long buildId);

    List<BuildStep> findByBuildId(Long buildId);

    List<BuildStep> findByBuildIdAndStatus(Long buildId, StepStatus status);

    List<BuildStep> findByBuildIdAndStatusAndUnresolvedDependenciesCount(Long buildId, StepStatus status, int count);

    @Modifying
    @Query("UPDATE BuildStep s SET s.unresolvedDependenciesCount = s.unresolvedDependenciesCount - 1 " +
           "WHERE s.build.id = :buildId AND s.name IN :stepNames AND s.unresolvedDependenciesCount > 0")
    int decrementUnresolvedDependencies(@Param("buildId") Long buildId, @Param("stepNames") Collection<String> stepNames);
}
