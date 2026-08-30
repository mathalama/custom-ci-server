package dev.mathalama.zovik.log.repository;

import dev.mathalama.zovik.log.domain.BuildLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildLogRepository extends JpaRepository<BuildLog, Long> {
    Page<BuildLog> findByBuildStepIdOrderByLineNumberAsc(Long buildStepId, Pageable pageable);
    List<BuildLog> findByBuildStepIdOrderByLineNumberAsc(Long buildStepId);
}
