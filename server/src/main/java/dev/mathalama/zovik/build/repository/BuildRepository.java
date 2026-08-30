package dev.mathalama.zovik.build.repository;

import dev.mathalama.zovik.build.domain.Build;
import dev.mathalama.zovik.build.domain.BuildStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BuildRepository extends JpaRepository<Build, Long> {
    Page<Build> findByProjectId(Long projectId, Pageable pageable);
    List<Build> findByStatus(BuildStatus status);
    List<Build> findByCreatedAtBefore(Instant threshold);
}
