package com.lowcode.bi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class EncryptionConfig {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;

    @Value("${app.security.encryption.key:your-32-byte-encryption-key-here-123456}")
    private String encryptionKey;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            SecretKeySpec keySpec = getKeySpec(salt);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + SALT_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(salt, 0, combined, IV_LENGTH, SALT_LENGTH);
            System.arraycopy(encryptedBytes, 0, combined, IV_LENGTH + SALT_LENGTH, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            if (combined.length < IV_LENGTH + SALT_LENGTH + GCM_TAG_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data format");
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, IV_LENGTH, salt, 0, SALT_LENGTH);

            byte[] encryptedBytes = new byte[combined.length - IV_LENGTH - SALT_LENGTH];
            System.arraycopy(combined, IV_LENGTH + SALT_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            SecretKeySpec keySpec = getKeySpec(salt);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("解密失败", e);
        }
    }

    private SecretKeySpec getKeySpec(byte[] salt) {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        byte[] combinedKey = new byte[keyBytes.length + salt.length];
        System.arraycopy(keyBytes, 0, combinedKey, 0, keyBytes.length);
        System.arraycopy(salt, 0, combinedKey, keyBytes.length, salt.length);

        byte[] aesKey = new byte[32];
        System.arraycopy(combinedKey, 0, aesKey, 0, Math.min(combinedKey.length, 32));

        return new SecretKeySpec(aesKey, "AES");
    }

    public String hash(String input) {
        if (input == null) {
            return null;
        }
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Hashing failed", e);
            throw new RuntimeException("哈希失败", e);
        }
    }
}
