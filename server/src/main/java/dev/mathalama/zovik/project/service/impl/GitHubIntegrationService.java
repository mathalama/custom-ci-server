package dev.mathalama.zovik.project.service.impl;

import dev.mathalama.zovik.system.domain.SystemSetting;
import dev.mathalama.zovik.system.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubIntegrationService {

    private final SecretCryptoService secretCryptoService;
    private final SystemSettingRepository systemSettingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${zovik.github.oauth.client-id:}")
    private String clientId;

    @Value("${zovik.github.oauth.client-secret:}")
    private String clientSecret;

    private static final String GITHUB_TOKEN_KEY = "github_oauth_token";

    public void saveToken(String token) {
        String encryptedToken = secretCryptoService.encrypt(token);
        SystemSetting setting = SystemSetting.builder()
                .key(GITHUB_TOKEN_KEY)
                .value(encryptedToken)
                .build();
        systemSettingRepository.save(setting);
        log.info("GitHub OAuth token saved and encrypted in database successfully.");
    }

    public String getToken() {
        return systemSettingRepository.findById(GITHUB_TOKEN_KEY)
                .map(setting -> secretCryptoService.decrypt(setting.getValue()))
                .orElse(null);
    }

    public boolean isConnected() {
        return systemSettingRepository.existsById(GITHUB_TOKEN_KEY);
    }

    public void disconnect() {
        String token = getToken();
        if (token != null) {
            revokeTokenOnGitHub(token);
        }
        systemSettingRepository.deleteById(GITHUB_TOKEN_KEY);
        log.info("GitHub OAuth token deleted from database successfully.");
    }

    private void revokeTokenOnGitHub(String token) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.warn("GitHub Client ID or Secret is missing, skipping remote token revocation.");
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            String auth = clientId + ":" + clientSecret;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + new String(encodedAuth, StandardCharsets.UTF_8));
            headers.set("Accept", "application/vnd.github+json");

            Map<String, String> body = new HashMap<>();
            body.put("access_token", token);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            String url = "https://api.github.com/applications/" + clientId + "/token";

            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("GitHub OAuth token revoked on GitHub side successfully.");
        } catch (Exception e) {
            log.warn("Failed to revoke GitHub OAuth token on GitHub side: {}", e.getMessage());
        }
    }
}
