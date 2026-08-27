package dev.mathalama.rabotyagaci.pipeline.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CacheDefinition(
        String key,
        List<String> paths,
        @JsonProperty("restore_keys") @JsonAlias({"restoreKeys", "restore_keys"}) List<String> restoreKeys
) {
    public CacheDefinition(List<String> paths) {
        this(null, paths, List.of());
    }

    public CacheDefinition(String key, List<String> paths) {
        this(key, paths, List.of());
    }
}
