package dev.mathalama.zovik.log.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Log sanitization service responsible for masking secrets and sensitive tokens
 * in build log streams and databases.
 *
 * Implements a streaming sliding-window overlap buffer to catch secrets that are
 * split across incoming chunk boundaries (e.g. SSE or WebSocket chunks).
 */
@Slf4j
@Service
public class LogSanitizer {

    public static final String MASK_REPLACEMENT = "***";
    public static final int MIN_SECRET_LENGTH = 4;

    private final Map<Long, StreamingSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Registers a sanitization session for an executing build step.
     */
    public void registerSession(Long stepId, Collection<String> rawSecrets) {
        if (stepId == null) return;
        List<String> validSecrets = filterAndSortSecrets(rawSecrets);
        activeSessions.put(stepId, new StreamingSession(validSecrets));
        log.debug("Registered log sanitizer session for step ID {} with {} secrets to mask", stepId, validSecrets.size());
    }

    /**
     * Processes a streaming chunk for a given step session using sliding window buffer.
     *
     * @return Safe string to emit immediately.
     */
    public String processChunk(Long stepId, String chunk) {
        if (chunk == null) return "";
        StreamingSession session = (stepId != null) ? activeSessions.get(stepId) : null;
        if (session == null) {
            return chunk;
        }
        return session.processChunk(chunk);
    }

    /**
     * Sanitizes a discrete line of text using the secrets registered for the step.
     */
    public String sanitizeLine(Long stepId, String line) {
        if (line == null || line.isEmpty()) return line;
        StreamingSession session = (stepId != null) ? activeSessions.get(stepId) : null;
        if (session == null || session.secrets.isEmpty()) {
            return line;
        }
        return mask(line, session.secrets);
    }

    /**
     * Flushes remaining buffered tail for a session and unregisters the session.
     *
     * @return Final sanitized tail to emit.
     */
    public String flushSession(Long stepId) {
        if (stepId == null) return "";
        StreamingSession session = activeSessions.remove(stepId);
        if (session == null) return "";
        return session.flush();
    }

    /**
     * Static / stateless masking of text with given collection of secrets.
     */
    public static String mask(String text, Collection<String> rawSecrets) {
        if (text == null || text.isEmpty() || rawSecrets == null || rawSecrets.isEmpty()) {
            return text;
        }
        List<String> sortedSecrets = filterAndSortSecrets(rawSecrets);
        if (sortedSecrets.isEmpty()) {
            return text;
        }

        String result = text;
        for (String secret : sortedSecrets) {
            result = result.replace(secret, MASK_REPLACEMENT);
        }
        return result;
    }

    /**
     * Filters out blank/short secrets and sorts them by length descending so longer
     * secrets are masked first before matching substrings.
     */
    public static List<String> filterAndSortSecrets(Collection<String> secrets) {
        if (secrets == null) return Collections.emptyList();
        return secrets.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> s.length() >= MIN_SECRET_LENGTH)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Internal streaming session tracking buffer and overlap for a single step.
     */
    public static class StreamingSession {
        private final List<String> secrets;
        private final int maxOverlap;
        private final StringBuilder buffer = new StringBuilder();

        public StreamingSession(List<String> secrets) {
            this.secrets = secrets != null ? secrets : Collections.emptyList();
            int maxLen = this.secrets.stream().mapToInt(String::length).max().orElse(0);
            this.maxOverlap = Math.max(0, maxLen - 1);
        }

        public synchronized String processChunk(String chunk) {
            if (secrets.isEmpty()) {
                return chunk;
            }

            buffer.append(chunk);
            String current = buffer.toString();
            String masked = mask(current, secrets);

            if (masked.length() <= maxOverlap) {
                buffer.setLength(0);
                buffer.append(masked);
                return "";
            }

            int emitEndIndex = masked.length() - maxOverlap;
            String safeToEmit = masked.substring(0, emitEndIndex);
            String tail = masked.substring(emitEndIndex);

            buffer.setLength(0);
            buffer.append(tail);
            return safeToEmit;
        }

        public synchronized String flush() {
            if (buffer.length() == 0) {
                return "";
            }
            String remaining = buffer.toString();
            buffer.setLength(0);
            return mask(remaining, secrets);
        }

        public List<String> getSecrets() {
            return secrets;
        }
    }
}
