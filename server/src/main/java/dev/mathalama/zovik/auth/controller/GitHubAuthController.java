package dev.mathalama.zovik.auth.controller;

import dev.mathalama.zovik.common.api.dto.ApiResponse;
import dev.mathalama.zovik.project.service.impl.GitHubIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/github")
@RequiredArgsConstructor
public class GitHubAuthController {

    @Value("${zovik.github.oauth.client-id:}")
    private String clientId;

    @Value("${zovik.github.oauth.client-secret:}")
    private String clientSecret;

    private final GitHubIntegrationService gitHubIntegrationService;
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
                    gitHubIntegrationService.saveToken(accessToken);
                    
                    Map<String, String> data = new HashMap<>();
                    data.put("status", "connected");
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

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getStatus() {
        Map<String, Boolean> data = new HashMap<>();
        data.put("connected", gitHubIntegrationService.isConnected());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnect() {
        gitHubIntegrationService.disconnect();
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> getProfile() {
        return proxyGitHubGet("https://api.github.com/user");
    }

    @GetMapping("/repos")
    public ResponseEntity<ApiResponse<Object>> getRepos() {
        return proxyGitHubGet("https://api.github.com/user/repos?per_page=100&sort=updated");
    }

    @GetMapping("/repos/{owner}/{repo}/branches")
    public ResponseEntity<ApiResponse<Object>> getBranches(@PathVariable String owner, @PathVariable String repo) {
        return proxyGitHubGet("https://api.github.com/repos/" + owner + "/" + repo + "/branches?per_page=100");
    }

    @GetMapping("/repos/{owner}/{repo}/contents")
    public ResponseEntity<ApiResponse<Object>> getContents(
            @PathVariable String owner, 
            @PathVariable String repo,
            @RequestParam(required = false) String ref) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents";
        if (ref != null && !ref.isBlank()) {
            url += "?ref=" + ref;
        }
        return proxyGitHubGet(url);
    }

    private ResponseEntity<ApiResponse<Object>> proxyGitHubGet(String url) {
        String token = gitHubIntegrationService.getToken();
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("GitHub integration is not connected"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/vnd.github+json");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            return ResponseEntity.ok(ApiResponse.ok(response.getBody()));
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("GitHub API returned 401 Unauthorized, disconnecting token: {}", e.getMessage());
            gitHubIntegrationService.disconnect();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("GitHub session expired, please reconnect your account."));
        } catch (Exception e) {
            log.error("Error proxying request to GitHub API (URL: {})", url, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.fail("Error calling GitHub API: " + e.getMessage()));
        }
    }

    public record CallbackRequest(String code) {}
}
