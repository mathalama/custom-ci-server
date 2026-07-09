package dev.mathalama.rabotyagaci.project.service.impl;

import dev.mathalama.rabotyagaci.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class SecretCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${rabotyagaci.security.master-key}")
    private String masterKeyString;

    private SecretKeySpec secretKey;

    @PostConstruct
    public void init() {
        if (masterKeyString == null || masterKeyString.length() < 16) {
            throw new IllegalStateException("Master key for encryption is too short or not set.");
        }
        // Use the first 32 bytes (256-bit) or 16 bytes (128-bit) of the key
        byte[] keyBytes = masterKeyString.getBytes(StandardCharsets.UTF_8);
        int keyLength = keyBytes.length >= 32 ? 32 : 16;
        byte[] truncatedKey = new byte[keyLength];
        System.arraycopy(keyBytes, 0, truncatedKey, 0, keyLength);
        this.secretKey = new SecretKeySpec(truncatedKey, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Prepend IV to ciphertext
            byte[] cipherTextWithIv = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, cipherTextWithIv, 0, iv.length);
            System.arraycopy(cipherText, 0, cipherTextWithIv, iv.length, cipherText.length);
            
            return Base64.getEncoder().encodeToString(cipherTextWithIv);
        } catch (Exception e) {
            log.error("Failed to encrypt secret", e);
            throw new BusinessException("Failed to encrypt secret", e);
        }
    }

    public String decrypt(String cipherTextWithIvBase64) {
        if (cipherTextWithIvBase64 == null) return null;
        try {
            byte[] cipherTextWithIv = Base64.getDecoder().decode(cipherTextWithIvBase64);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(cipherTextWithIv, 0, iv, 0, iv.length);
            
            int cipherTextLength = cipherTextWithIv.length - iv.length;
            byte[] cipherText = new byte[cipherTextLength];
            System.arraycopy(cipherTextWithIv, iv.length, cipherText, 0, cipherTextLength);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt secret. The data may be corrupted or the encryption key is incorrect.", e);
            throw new IllegalStateException("Decryption failed. Refusing to return potentially corrupted secret data.", e);
        }
    }
}
