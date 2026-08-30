package dev.mathalama.zovik.pipeline.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "zovik.git")
public class PipelineConfig {

    /**
     * Path where repositories will be cloned.
     */
    private String cloneDir;
}
