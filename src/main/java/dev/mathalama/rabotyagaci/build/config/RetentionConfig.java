package dev.mathalama.rabotyagaci.build.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rabotyagaci.retention")
public class RetentionConfig {
    private int buildsDays = 30;
    private String cron = "0 0 4 * * *";
}
