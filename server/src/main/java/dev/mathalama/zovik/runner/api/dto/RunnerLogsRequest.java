package dev.mathalama.zovik.runner.api.dto;

import java.util.List;

public record RunnerLogsRequest(
        int fromLine,
        List<String> lines,
        String stream
) {}
