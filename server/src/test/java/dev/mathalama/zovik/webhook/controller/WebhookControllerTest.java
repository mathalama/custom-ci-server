package dev.mathalama.zovik.webhook.controller;

import dev.mathalama.zovik.common.exception.BusinessException;
import dev.mathalama.zovik.webhook.service.impl.GitHubWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubWebhookService gitHubWebhookService;

    @Test
    void handleGitHubWebhook_ShouldReturn202_WhenPayloadProcessed() throws Exception {
        Long projectId = 1L;
        String signature = "sha256=testsignature";
        String payload = "{\"ref\":\"refs/heads/main\"}";

        doNothing().when(gitHubWebhookService).processPayload(projectId, signature, payload);

        mockMvc.perform(post("/api/v1/webhooks/github/{projectId}", projectId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Webhook processed successfully"))
                .andExpect(jsonPath("$.error").isEmpty());

        verify(gitHubWebhookService, times(1)).processPayload(projectId, signature, payload);
    }

    @Test
    void handleGitHubWebhook_ShouldReturn409_WhenSignatureInvalid() throws Exception {
        Long projectId = 1L;
        String signature = "sha256=invalidsignature";
        String payload = "{\"ref\":\"refs/heads/main\"}";

        doThrow(new BusinessException("Invalid webhook signature"))
                .when(gitHubWebhookService).processPayload(projectId, signature, payload);

        mockMvc.perform(post("/api/v1/webhooks/github/{projectId}", projectId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid webhook signature"));

        verify(gitHubWebhookService, times(1)).processPayload(projectId, signature, payload);
    }
}
