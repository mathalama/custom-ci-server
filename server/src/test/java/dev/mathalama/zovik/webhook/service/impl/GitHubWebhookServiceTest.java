package dev.mathalama.zovik.webhook.service.impl;

import dev.mathalama.zovik.common.exception.BusinessException;
import dev.mathalama.zovik.common.exception.ResourceNotFoundException;
import dev.mathalama.zovik.project.domain.GitProvider;
import dev.mathalama.zovik.project.domain.Project;
import dev.mathalama.zovik.project.repository.ProjectRepository;
import dev.mathalama.zovik.webhook.api.event.WebhookReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubWebhookServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GitHubWebhookService gitHubWebhookService;

    private Project testProject;
    private final String webhookSecret = "super-secret-key-123";

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id(1L)
                .name("test-project")
                .repoUrl("https://github.com/test/repo")
                .gitProvider(GitProvider.GITHUB)
                .defaultBranch("main")
                .webhookSecret(webhookSecret)
                .isActive(true)
                .build();
    }

    @Test
    void testProcessPayloadSuccess() throws Exception {
        String payload = """
                {
                  "ref": "refs/heads/main",
                  "after": "6113728f27ae82c3b1e177c8d37d402b743f6351",
                  "head_commit": {
                    "id": "6113728f27ae82c3b1e177c8d37d402b743f6351",
                    "message": "Update README.md"
                  }
                }
                """;

        String signature = calculateHmac(payload, webhookSecret);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        gitHubWebhookService.processPayload(1L, signature, payload);

        ArgumentCaptor<WebhookReceivedEvent> eventCaptor = ArgumentCaptor.forClass(WebhookReceivedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        WebhookReceivedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(1L, publishedEvent.projectId());
        assertEquals("main", publishedEvent.branch());
        assertEquals("6113728f27ae82c3b1e177c8d37d402b743f6351", publishedEvent.commitSha());
        assertEquals("Update README.md", publishedEvent.commitMessage());
    }

    @Test
    void testProcessPayloadInvalidSignature() {
        String payload = """
                {
                  "ref": "refs/heads/main",
                  "after": "6113728f27ae82c3b1e177c8d37d402b743f6351"
                }
                """;

        String invalidSignature = "sha256=invalidhashvalue1234567890";

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        assertThrows(BusinessException.class, () ->
                gitHubWebhookService.processPayload(1L, invalidSignature, payload)
        );

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testProcessPayloadIgnoreBranchDeletion() throws Exception {
        String payload = """
                {
                  "ref": "refs/heads/feature-branch",
                  "after": "0000000000000000000000000000000000000000"
                }
                """;

        String signature = calculateHmac(payload, webhookSecret);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        gitHubWebhookService.processPayload(1L, signature, payload);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testProcessPayloadIgnoreNonPushEvent() throws Exception {
        String payload = """
                {
                  "zen": "Non-cooperation with evil is as much a duty as is cooperation with good.",
                  "hook_id": 123456789
                }
                """;

        String signature = calculateHmac(payload, webhookSecret);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        gitHubWebhookService.processPayload(1L, signature, payload);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testProcessPayloadProjectNotFound() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                gitHubWebhookService.processPayload(999L, "sig", "payload")
        );
    }

    @Test
    void testProcessPayloadProjectInactive() {
        testProject.setActive(false);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        assertThrows(BusinessException.class, () ->
                gitHubWebhookService.processPayload(1L, "sig", "payload")
        );
    }

    private String calculateHmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(rawHmac);
    }
}
