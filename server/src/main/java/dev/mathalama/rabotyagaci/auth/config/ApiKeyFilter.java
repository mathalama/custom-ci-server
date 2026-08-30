package dev.mathalama.rabotyagaci.auth.config;

import java.io.IOException;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.mathalama.rabotyagaci.common.api.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@ConditionalOnProperty(name = "rabotyagaci.auth.enabled", havingValue = "true")
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String apiKey;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JsonMapper jsonMapper = new JsonMapper();

    public ApiKeyFilter(@Value("${rabotyagaci.auth.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Bypass security for webhooks (requires wildcard), callback, config, health, and system info
        if (pathMatcher.match("/api/v1/webhooks/**", uri) 
                || "/api/v1/auth/github/config".equals(uri)
                || "/api/v1/auth/github/callback".equals(uri)
                || "/api/v1/system/info".equals(uri)
                || "/actuator/health".equals(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestKey = request.getHeader("X-API-Key");
        if (apiKey.equals(requestKey)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Unauthorized request to URL: {}", uri);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> apiResponse = new ApiResponse<>(
                    false,
                    null,
                    "Unauthorized: Invalid or missing X-API-Key header",
                    Instant.now()
            );

            response.getWriter().write(jsonMapper.writeValueAsString(apiResponse));
        }
    }
}
