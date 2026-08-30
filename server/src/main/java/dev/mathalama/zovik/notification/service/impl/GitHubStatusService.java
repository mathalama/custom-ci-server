package dev.mathalama.zovik.notification.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class GitHubStatusService {

    private final RestClient restClient;

    public GitHubStatusService() {
        this.restClient = RestClient.create("https://api.github.com");
    }

    public void updateCommitStatus(String repoUrl, String commitSha, String status, String description, String targetUrl, String token) {
        if (token == null || token.isBlank()) {
            log.debug("GitHub token not configured, skipping status update for commit {}", commitSha);
            return;
        }

        String ownerAndRepo = extractOwnerAndRepo(repoUrl);
        if (ownerAndRepo == null) {
            log.warn("Could not extract owner/repo from URL: {}", repoUrl);
            return;
        }

        String url = "/repos/" + ownerAndRepo + "/statuses/" + commitSha;

        GitHubStatusRequest request = new GitHubStatusRequest(status, targetUrl, description, "Zovik");

        try {
            restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully sent GitHub status '{}' for commit {}", status, commitSha);
        } catch (Exception e) {
            log.error("Failed to send GitHub status for commit {}: {}", commitSha, e.getMessage());
        }
    }

    private String extractOwnerAndRepo(String repoUrl) {
        try {
            String cleanUrl = repoUrl.replace(".git", "");
            if (cleanUrl.startsWith("https://github.com/")) {
                return cleanUrl.substring("https://github.com/".length());
            } else if (cleanUrl.startsWith("git@github.com:")) {
                return cleanUrl.substring("git@github.com:".length());
            }
        } catch (Exception e) {
            log.warn("Error parsing repo URL: {}", repoUrl);
        }
        return null;
    }

    private record GitHubStatusRequest(String state, String target_url, String description, String context) {}
}
