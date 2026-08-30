package dev.mathalama.zovik.project.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "git_provider", nullable = false)
    private GitProvider gitProvider;

    @Builder.Default
    @Column(name = "default_branch", nullable = false)
    private String defaultBranch = "main";

    @Column(name = "webhook_secret", nullable = false)
    private String webhookSecret;

    @Column(name = "github_token")
    private String githubToken;

    @Builder.Default
    @Column(name = "pipeline_config_path", nullable = false)
    private String pipelineConfigPath = ".zovik.yml";

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectSecret> secrets = new java.util.ArrayList<>();
}
