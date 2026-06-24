package dev.mathalama.rabotyagaci.webhook.controller;

import dev.mathalama.rabotyagaci.common.api.dto.ApiResponse;
import dev.mathalama.rabotyagaci.webhook.service.impl.GitHubWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final GitHubWebhookService gitHubWebhookService;

    @PostMapping("/github/{projectId}")
    public ResponseEntity<ApiResponse<String>> handleGitHubWebhook(
            @PathVariable Long projectId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody(required = false) String payload
    ) {
        log.info("Received GitHub webhook for project ID: {}. Payload is null? {}", projectId, (payload == null));
        if (payload == null) {
            log.warn("Payload is empty! Returning 200 to satisfy GitHub ping.");
            return ResponseEntity.ok(new ApiResponse<>(true, "Empty payload ignored", null, Instant.now()));
        }
        gitHubWebhookService.processPayload(projectId, signatureHeader, payload);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                "Webhook processed successfully",
                null,
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
