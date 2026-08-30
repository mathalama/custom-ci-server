package dev.mathalama.zovik.runner.api.dto;

public record RunnerRegisterRequest(
        String registrationToken,
        String name,
        String os,
        Integer cpuCores,
        Long memoryBytes
) {}
