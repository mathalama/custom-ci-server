package dev.mathalama.rabotyagaci.build.domain;

import dev.mathalama.rabotyagaci.project.domain.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "builds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Build {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Column(nullable = false, length = 100)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuildStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "notify_on_success", length = 1000)
    private String notifyOnSuccess;

    @Column(name = "notify_on_failure", length = 1000)
    private String notifyOnFailure;

    @Builder.Default
    @OneToMany(mappedBy = "build", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<BuildStep> steps = new ArrayList<>();
}
