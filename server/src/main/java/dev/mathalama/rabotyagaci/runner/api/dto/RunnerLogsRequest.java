package dev.mathalama.rabotyagaci.runner.api.dto;

import java.util.List;

public record RunnerLogsRequest(
        int fromLine,
        List<String> lines,
        String stream
) {}
