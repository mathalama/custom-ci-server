package dev.mathalama.rabotyagaci.artifact.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rabotyagaci.artifacts")
public class ArtifactConfig {
    private String storageDir;
    private int retentionDays = 30;
}
