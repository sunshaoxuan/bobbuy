package com.bobbuy.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utility class for AES-256-GCM decryption and PBKDF2 key derivation.
 * Used for secure management of AI API keys.
 */
public class EncryptionUtils {

    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 256;
    private static final int TAG_LENGTH_BITS = 128;

    /**
     * Decrypts a payload using AES-256-GCM with a PBKDF2-derived key.
     *
     * @param password    The decryption password.
     * @param saltBase64  Base64 encoded salt for KDF.
     * @param nonceBase64 Base64 encoded nonce (IV).
     * @param cipherBase64 Base64 encoded ciphertext.
     * @param tagBase64   Base64 encoded authentication tag.
     * @param iterations  Number of PBKDF2 iterations.
     * @return The plaintext string.
     * @throws Exception if decryption fails.
     */
    public static String decrypt(String password, String saltBase64, String nonceBase64, 
                                String cipherBase64, String tagBase64, int iterations) throws Exception {
        
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] nonce = Base64.getDecoder().decode(nonceBase64);
        byte[] ciphertext = Base64.getDecoder().decode(cipherBase64);
        byte[] tag = Base64.getDecoder().decode(tagBase64);

        // 1. Derive Key
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        // 2. Prepare Payload (Ciphertext + Tag)
        // JCE GCM implementation expects the tag to be appended to the ciphertext
        byte[] encryptedWithTag = new byte[ciphertext.length + tag.length];
        System.arraycopy(ciphertext, 0, encryptedWithTag, 0, ciphertext.length);
        System.arraycopy(tag, 0, encryptedWithTag, ciphertext.length, tag.length);

        // 3. Decrypt
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, nonce);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        byte[] plaintext = cipher.doFinal(encryptedWithTag);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
