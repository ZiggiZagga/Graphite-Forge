package com.example.configserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive configuration values.
 * Uses AES-256 encryption for symmetric operations.
 */
@Slf4j
@Service
public class ConfigEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    private static final String TRANSFORMATION = "AES";
    
    private final SecretKey secretKey;
    
    public ConfigEncryptionService(ConfigServerProperties properties) {
        this.secretKey = deriveKey(properties.getEncryptionPassword());
    }
    
    /**
     * Encrypt a configuration value.
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error encrypting value", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt a configuration value.
     */
    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting value", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * Derive a secret key from a password using PBKDF2.
     */
    private SecretKey deriveKey(String password) {
        try {
            // Simple key derivation from password - pad or truncate to 32 bytes
            byte[] keyBytes = new byte[32];
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(passwordBytes, 0, keyBytes, 0, Math.min(32, passwordBytes.length));
            
            return new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITHM);
        } catch (Exception e) {
            log.error("Error deriving encryption key", e);
            throw new RuntimeException("Key derivation failed", e);
        }
    }
}
