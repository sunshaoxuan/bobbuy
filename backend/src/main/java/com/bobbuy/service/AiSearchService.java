package com.bobbuy.service;

import com.bobbuy.util.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Service for executing AI-driven web research using secure API keys.
 */
@Service
public class AiSearchService {
    private static final Logger log = LoggerFactory.getLogger(AiSearchService.class);

    @Value("${bobbuy.ai.secret.brave.salt}")
    private String braveSalt;
    @Value("${bobbuy.ai.secret.brave.nonce}")
    private String braveNonce;
    @Value("${bobbuy.ai.secret.brave.ciphertext}")
    private String braveCiphertext;
    @Value("${bobbuy.ai.secret.brave.tag}")
    private String braveTag;
    @Value("${bobbuy.ai.secret.brave.iterations}")
    private int braveIterations;

    // The master password should be provided via environment variable for security
    @Value("${AI_ENCRYPTION_PWD:Nho#123456}") 
    private String masterPassword;

    private String decryptedBraveKey;

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing AiSearchService: Decrypting Brave Search API Key...");
            this.decryptedBraveKey = EncryptionUtils.decrypt(
                masterPassword, braveSalt, braveNonce, braveCiphertext, braveTag, braveIterations
            );
            log.info("Brave Search API Key successfully decrypted.");
        } catch (Exception e) {
            log.error("CRITICAL: Failed to decrypt Brave Search API Key. Web research capabilities will be disabled.", e);
        }
    }

    public String search(String query) {
        if (decryptedBraveKey == null) {
            log.warn("Search attempted but Brave API key is missing or failed to decrypt.");
            return "";
        }
        // Implementation for Brave Search API call would go here
        log.info("Executing Brave Search for: {}", query);
        return "Stub results for: " + query;
    }
}
