package dev.mathalama.rabotyagaci.notification.service.impl;

import dev.mathalama.rabotyagaci.build.api.event.BuildCompletedEvent;
import dev.mathalama.rabotyagaci.build.domain.Build;
import dev.mathalama.rabotyagaci.build.domain.BuildStatus;
import dev.mathalama.rabotyagaci.build.repository.BuildRepository;
import dev.mathalama.rabotyagaci.notification.api.NotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private final BuildRepository buildRepository;
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${rabotyagaci.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${rabotyagaci.notification.email.from:}")
    private String emailFrom;

    @Value("${rabotyagaci.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void notify(Long buildId, BuildStatus status, String message) {
        // Obsolete plain text notification
    }

    @EventListener
    public void handleBuildCompleted(BuildCompletedEvent event) {
        if (!emailEnabled) {
            return;
        }

        Build build = buildRepository.findById(event.buildId()).orElse(null);
        if (build == null) {
            return;
        }

        List<String> targetEmails = determineRecipients(build, event.status());

        if (targetEmails.isEmpty()) {
            log.info("No email recipients configured for build {}", build.getId());
            return;
        }

        String projectName = build.getProject().getName();
        String branch = build.getBranch();
        String buildUrl = frontendUrl + "/builds/" + event.buildId();

        try {
            Context context = new Context();
            context.setVariable("projectName", projectName);
            context.setVariable("branch", branch);
            context.setVariable("buildId", event.buildId());
            context.setVariable("status", event.status().name());
            context.setVariable("buildUrl", buildUrl);

            String htmlContent = templateEngine.process("email/build-notification", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(targetEmails.toArray(new String[0]));
            helper.setSubject(String.format("RabotyagaCI: Build #%d [%s]", event.buildId(), event.status().name()));
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("HTML email notification sent to {} for build {}", targetEmails, event.buildId());
        } catch (Exception e) {
            log.error("Failed to send HTML email notification for build {}: {}", event.buildId(), e.getMessage());
        }
    }

    private List<String> determineRecipients(Build build, BuildStatus status) {
        String configuredEmailsStr = null;

        if (status == BuildStatus.SUCCESS) {
            configuredEmailsStr = build.getNotifyOnSuccess();
        } else if (status == BuildStatus.FAILURE || status == BuildStatus.CANCELLED) {
            configuredEmailsStr = build.getNotifyOnFailure();
        }

        List<String> recipients = new ArrayList<>();

        if (configuredEmailsStr != null && !configuredEmailsStr.isBlank()) {
            recipients = Arrays.stream(configuredEmailsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        // Resolve "author" keyword
        return recipients.stream()
                .map(email -> {
                    if ("author".equalsIgnoreCase(email)) {
                        return build.getAuthorEmail();
                    }
                    return email;
                })
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
