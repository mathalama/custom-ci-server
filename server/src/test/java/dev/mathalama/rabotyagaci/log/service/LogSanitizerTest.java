package dev.mathalama.rabotyagaci.log.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    private LogSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new LogSanitizer();
    }

    @Test
    @DisplayName("Should mask multiple full secrets in a single string")
    void testBasicMasking() {
        String input = "Connecting with token ghp_superSecretToken123 and password MyDatabasePass456!";
        List<String> secrets = List.of("ghp_superSecretToken123", "MyDatabasePass456!");

        String result = LogSanitizer.mask(input, secrets);

        assertThat(result).isEqualTo("Connecting with token *** and password ***");
    }

    @Test
    @DisplayName("Should mask secret split across two streaming chunks")
    void testSecretSplitAcrossTwoChunks() {
        Long stepId = 101L;
        String secret = "superSecretKey987654";
        sanitizer.registerSession(stepId, Set.of(secret));

        // Chunk 1 contains first half of secret: "superSec"
        String chunk1 = "Running command with key superSec";
        String out1 = sanitizer.processChunk(stepId, chunk1);

        // Chunk 2 contains second half of secret: "retKey987654 --verbose"
        String chunk2 = "retKey987654 --verbose\n";
        String out2 = sanitizer.processChunk(stepId, chunk2);

        String flush = sanitizer.flushSession(stepId);

        String fullStreamOutput = out1 + out2 + flush;

        assertThat(fullStreamOutput).doesNotContain(secret);
        assertThat(fullStreamOutput).contains("Running command with key *** --verbose\n");
    }

    @Test
    @DisplayName("Should mask secret split across three fine-grained streaming chunks")
    void testSecretSplitAcrossThreeChunks() {
        Long stepId = 202L;
        String secret = "AWS_SECRET_ACCESS_KEY_XYZ123";
        sanitizer.registerSession(stepId, Set.of(secret));

        String chunk1 = "export AWS_SECRET_ACCESS_KEY=";
        String chunk2 = "AWS_SECRET_";
        String chunk3 = "ACCESS_KEY_XYZ123 and run";

        String out1 = sanitizer.processChunk(stepId, chunk1);
        String out2 = sanitizer.processChunk(stepId, chunk2);
        String out3 = sanitizer.processChunk(stepId, chunk3);
        String flush = sanitizer.flushSession(stepId);

        String total = out1 + out2 + out3 + flush;

        assertThat(total).doesNotContain(secret);
        assertThat(total).contains("export AWS_SECRET_ACCESS_KEY=*** and run");
    }

    @Test
    @DisplayName("Should ignore secrets shorter than minimum length to avoid over-masking common words")
    void testShortSecretsIgnored() {
        String input = "The quick brown fox jumps over the lazy dog";
        List<String> secrets = List.of("The", "a", "ox", ""); // All < 4 chars

        String result = LogSanitizer.mask(input, secrets);

        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("Should prioritize longer secrets before shorter substrings")
    void testLongerSecretsPrecedence() {
        String input = "token: secret_key_admin_extended";
        List<String> secrets = List.of("secret_key", "secret_key_admin_extended");

        String result = LogSanitizer.mask(input, secrets);

        assertThat(result).isEqualTo("token: ***");
    }
}
