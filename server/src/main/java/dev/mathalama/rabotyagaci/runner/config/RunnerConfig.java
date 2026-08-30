package dev.mathalama.rabotyagaci.runner.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rabotyagaci.runner")
public class RunnerConfig {

    private String dockerHost;
    private String workspaceDir;
    private String volumeName = "zovik_app-data";
    private long defaultTimeout = 600L;
    private int maxConcurrentBuilds = 3;
    private String containerMemoryLimit = "512m";
    private double containerCpuLimit = 1.0;
    private String networkMode = "bridge";
}
