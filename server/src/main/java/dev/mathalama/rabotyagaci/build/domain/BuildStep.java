package dev.mathalama.rabotyagaci.build.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "build_steps")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_id", nullable = false)
    private Build build;

    @Column(nullable = false)
    private String name;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "docker_image", nullable = false)
    private String dockerImage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String commands;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(nullable = false)
    private boolean privileged;

    @Column(name = "docker_socket", nullable = false)
    private boolean dockerSocket;

    @Column(name = "secret_files", columnDefinition = "TEXT")
    private String secretFiles;

    @Column(name = "depends_on", columnDefinition = "TEXT")
    private String dependsOn;

    @Column(name = "unresolved_dependencies_count", nullable = false)
    private int unresolvedDependenciesCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runner_id")
    private dev.mathalama.rabotyagaci.runner.domain.Runner runner;
}
