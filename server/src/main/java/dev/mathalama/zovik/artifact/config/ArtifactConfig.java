package dev.mathalama.zovik.artifact.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "zovik.artifacts")
public class ArtifactConfig {
    private String storageDir;
    private int retentionDays = 30;
}
