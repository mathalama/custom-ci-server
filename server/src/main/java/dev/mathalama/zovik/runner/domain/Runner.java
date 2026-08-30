package dev.mathalama.zovik.runner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "runners")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Runner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String host;

    @Column
    private Integer port;

    @Column
    private String username;

    @Column(name = "ssh_key", columnDefinition = "TEXT")
    private String sshKey;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunnerStatus status;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "registration_token")
    private String registrationToken;

    @Column(name = "registration_token_expires_at")
    private Instant registrationTokenExpiresAt;

    @Column
    private String os;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "memory_bytes")
    private Long memoryBytes;

    @Column(name = "install_log", columnDefinition = "TEXT")
    private String installLog;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
