package dev.mathalama.zovik.project.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SecretCryptoServiceTest {

    private SecretCryptoService secretCryptoService;

    @BeforeEach
    void setUp() {
        secretCryptoService = new SecretCryptoService();
        ReflectionTestUtils.setField(secretCryptoService, "masterKeyString", "test-master-key-1234567890123456");
        secretCryptoService.init();
    }

    @Test
    void testEncryptDecryptHappyPath() {
        String plainText = "my-super-secret-password-123!";
        
        String cipherText = secretCryptoService.encrypt(plainText);
        
        assertNotNull(cipherText);
        assertNotEquals(plainText, cipherText);
        
        String decryptedText = secretCryptoService.decrypt(cipherText);
        assertEquals(plainText, decryptedText);
    }

    @Test
    void testEncryptProducesDifferentCiphertextsForSameInput() {
        String plainText = "same-input";
        
        String cipherText1 = secretCryptoService.encrypt(plainText);
        String cipherText2 = secretCryptoService.encrypt(plainText);
        
        assertNotNull(cipherText1);
        assertNotNull(cipherText2);
        
        // Extract IVs and compare them explicitly
        byte[] decoded1 = java.util.Base64.getDecoder().decode(cipherText1);
        byte[] decoded2 = java.util.Base64.getDecoder().decode(cipherText2);
        
        byte[] iv1 = new byte[12];
        byte[] iv2 = new byte[12];
        System.arraycopy(decoded1, 0, iv1, 0, 12);
        System.arraycopy(decoded2, 0, iv2, 0, 12);
        
        assertFalse(java.util.Arrays.equals(iv1, iv2), "Extracted IVs must be different for consecutive encryptions");
        
        assertEquals(plainText, secretCryptoService.decrypt(cipherText1));
        assertEquals(plainText, secretCryptoService.decrypt(cipherText2));
    }

    @Test
    void testDecryptCorruptedDataThrowsException() {
        String plainText = "sensitive-data";
        String cipherText = secretCryptoService.encrypt(plainText);
        
        // Corrupt the ciphertext by flipping a bit in the authentication tag (last 16 bytes)
        byte[] decoded = java.util.Base64.getDecoder().decode(cipherText);
        decoded[decoded.length - 1] = (byte) (decoded[decoded.length - 1] ^ 0xFF);
        String corruptedCipherText = java.util.Base64.getEncoder().encodeToString(decoded);
        
        // It should catch AEADBadTagException and throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            secretCryptoService.decrypt(corruptedCipherText);
        });
        assertTrue(ex.getMessage().contains("Decryption failed"));
    }

    @Test
    void testDecryptInvalidDataThrowsException() {
        String invalidData = "not-base64-encoded-and-not-encrypted";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            secretCryptoService.decrypt(invalidData);
        });
        assertTrue(ex.getMessage().contains("Decryption failed"));
    }
    
    @Test
    void testInitFailsIfKeyTooShort() {
        SecretCryptoService service = new SecretCryptoService();
        ReflectionTestUtils.setField(service, "masterKeyString", "short");
        
        IllegalStateException ex = assertThrows(IllegalStateException.class, service::init);
        assertTrue(ex.getMessage().contains("too short"));
    }
}
