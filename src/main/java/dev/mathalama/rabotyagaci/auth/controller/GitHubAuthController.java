package dev.mathalama.rabotyagaci.auth.controller;

import dev.mathalama.rabotyagaci.common.api.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/github")
@RequiredArgsConstructor
public class GitHubAuthController {

    @Value("${rabotyagaci.github.oauth.client-id:}")
    private String clientId;

    @Value("${rabotyagaci.github.oauth.client-secret:}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("clientId", clientId);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Map<String, String>>> handleCallback(@RequestBody CallbackRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Authorization code must not be empty"));
        }

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.error("GitHub OAuth is not configured on the backend. Client ID or Client Secret is missing.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("GitHub OAuth is not configured on this server"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("client_id", clientId);
            requestBody.put("client_secret", clientSecret);
            requestBody.put("code", request.code());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://github.com/login/oauth/access_token",
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = response.getBody();
                if (body.containsKey("error")) {
                    String errorDesc = (String) body.get("error_description");
                    log.warn("GitHub OAuth token exchange failed: {}", errorDesc);
                    return ResponseEntity.badRequest().body(ApiResponse.fail("OAuth exchange failed: " + errorDesc));
                }

                String accessToken = (String) body.get("access_token");
                if (accessToken != null) {
                    Map<String, String> data = new HashMap<>();
                    data.put("accessToken", accessToken);
                    return ResponseEntity.ok(ApiResponse.ok(data));
                }
            }

            return ResponseEntity.badRequest().body(ApiResponse.fail("Failed to retrieve access token from GitHub"));

        } catch (Exception e) {
            log.error("Error during GitHub OAuth token exchange", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Internal server error during OAuth exchange: " + e.getMessage()));
        }
    }

    public record CallbackRequest(String code) {}
}
