package dev.mathalama.rabotyagaci.runner.repository;

import dev.mathalama.rabotyagaci.runner.domain.Runner;
import dev.mathalama.rabotyagaci.runner.domain.RunnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RunnerRepository extends JpaRepository<Runner, Long> {

    Optional<Runner> findByToken(String token);

    Optional<Runner> findByRegistrationToken(String registrationToken);

    List<Runner> findByStatus(RunnerStatus status);

    List<Runner> findByStatusNotAndLastSeenAtBefore(RunnerStatus status, Instant cutoff);
}
