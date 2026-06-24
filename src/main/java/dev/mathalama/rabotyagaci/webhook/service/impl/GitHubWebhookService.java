package dev.mathalama.rabotyagaci.webhook.service.impl;

import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import dev.mathalama.rabotyagaci.webhook.api.event.WebhookReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubWebhookService {

    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final JsonMapper jsonMapper = new JsonMapper();

    @Transactional(readOnly = true)
    public void processPayload(Long projectId, String signatureHeader, String payload) {
        log.info("Processing webhook for project ID: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        if (!project.isActive()) {
            throw new BusinessException("Cannot process webhook for inactive project: " + project.getName());
        }

        // Verify HMAC-SHA256 signature
        if (project.getWebhookSecret() != null && !project.getWebhookSecret().isBlank()) {
            if (!isValidSignature(payload, signatureHeader, project.getWebhookSecret())) {
                log.warn("Invalid HMAC signature for project ID: {}", projectId);
                throw new BusinessException("Invalid webhook signature");
            }
        }

        try {
            JsonNode root = jsonMapper.readTree(payload);

            // GitHub push payloads contain "ref" and "after"
            if (!root.has("ref") || !root.has("after")) {
                log.info("Ignoring webhook payload: missing 'ref' or 'after' fields (non-push event)");
                return;
            }

            String ref = root.get("ref").asText();
            String commitSha = root.get("after").asText();

            // Ignore deletion events (where 'after' is 40 zeros)
            if ("0000000000000000000000000000000000000000".equals(commitSha)) {
                log.info("Ignoring branch deletion webhook event for project ID: {}", projectId);
                return;
            }

            // Extract branch name from ref (e.g. "refs/heads/main" -> "main")
            String branch = ref;
            if (ref.startsWith("refs/heads/")) {
                branch = ref.substring(11);
            }

            // Extract commit message from head_commit
            String commitMessage = "";
            if (root.has("head_commit") && !root.get("head_commit").isNull()) {
                JsonNode headCommit = root.get("head_commit");
                if (headCommit.has("message")) {
                    commitMessage = headCommit.get("message").asText();
                }
            }

            log.info("Successfully parsed push webhook: branch={}, commitSha={}, commitMessage='{}'",
                    branch, commitSha, commitMessage);

            // Publish WebhookReceivedEvent
            eventPublisher.publishEvent(new WebhookReceivedEvent(
                    projectId,
                    branch,
                    commitSha,
                    commitMessage,
                    Instant.now()
            ));

        } catch (Exception e) {
            log.error("Failed to parse GitHub webhook payload", e);
            throw new BusinessException("Failed to parse webhook payload: " + e.getMessage(), e);
        }
    }

    private boolean isValidSignature(String payload, String signatureHeader, String secret) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expectedHash = signatureHeader.substring(7);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String actualHash = HexFormat.of().formatHex(rawHmac);
            return MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), actualHash.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
}
