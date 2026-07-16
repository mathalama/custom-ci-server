package dev.mathalama.rabotyagaci.runner.repository;

import dev.mathalama.rabotyagaci.runner.domain.Runner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RunnerRepository extends JpaRepository<Runner, Long> {
    Optional<Runner> findByToken(String token);
}
