package dev.mathalama.zovik.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for Spring Boot.
 * Enables caching using the simple in-memory cache manager.
 * Projects are cached by page number and size to optimize Dashboard load.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
