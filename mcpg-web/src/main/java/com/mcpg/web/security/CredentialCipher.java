package com.mcpg.web.security;

import com.mcpg.core.exception.McpgException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts registry passwords at rest with AES-GCM.
 *
 * <p>Ciphertext is stored as {@code enc:v1:&lt;base64(iv+ciphertext)&gt;}.
 * Plain values without the prefix are treated as legacy plaintext and
 * re-encrypted on the next write.</p>
 */
@Component
public class CredentialCipher {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final SecurityProperties properties;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(SecurityProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        if (plaintext.startsWith(PREFIX)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, currentKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + encrypted.length);
            buf.put(iv);
            buf.put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new McpgException("Failed to encrypt credential: " + e.getMessage(), e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        if (!stored.startsWith(PREFIX)) {
            return stored; // legacy plaintext
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LEN];
            byte[] ciphertext = new byte[all.length - IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            System.arraycopy(all, IV_LEN, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, currentKey(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new McpgException("Failed to decrypt credential (wrong MCPG_SECRET_KEY?): "
                    + e.getMessage(), e);
        }
    }

    private SecretKey currentKey() {
        return new SecretKeySpec(deriveKeyBytes(), "AES");
    }

    private byte[] deriveKeyBytes() {
        try {
            String material = properties.getSecretKey();
            if (material == null || material.isBlank()) {
                material = properties.getApiToken();
            }
            if (material == null || material.isBlank()) {
                material = "mcpg-dev-insecure-default-key";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
