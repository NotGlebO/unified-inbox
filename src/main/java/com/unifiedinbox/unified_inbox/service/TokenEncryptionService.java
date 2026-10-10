
package com.unifiedinbox.unified_inbox.service;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.encryption.key}")
    private String encryptionKey;

    private SecretKeySpec secretKey;

    @PostConstruct
    public void initialize() {

        byte[] keyBytes = Base64.getDecoder()
                .decode(encryptionKey);

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Encryption key must be 32 bytes."
            );
        }

        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH, iv)
            );

            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            byte[] result = new byte[iv.length + encrypted.length];

            System.arraycopy(
                    iv, 0,
                    result, 0,
                    iv.length
            );

            System.arraycopy(
                    encrypted, 0,
                    result, iv.length,
                    encrypted.length
            );

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to encrypt token.", e
            );
        }
    }

    public String decrypt(String encryptedText) {

        try {
            byte[] data = Base64.getDecoder()
                    .decode(encryptedText);

            if (data.length < IV_LENGTH + 16) {
                throw new IllegalArgumentException(
                        "Invalid encrypted token."
                );
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[data.length - IV_LENGTH];

            System.arraycopy(
                    data, 0,
                    iv, 0,
                    IV_LENGTH
            );

            System.arraycopy(
                    data, IV_LENGTH,
                    encrypted, 0,
                    encrypted.length
            );

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH, iv)
            );

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to decrypt token.", e
            );
        }
    }
}
