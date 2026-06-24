package dev.mathalama.rabotyagaci.log.domain;

import dev.mathalama.rabotyagaci.build.domain.BuildStep;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "build_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_step_id", nullable = false)
    private BuildStep buildStep;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogStream stream;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;
}
