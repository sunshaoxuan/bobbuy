package com.bobbuy.service;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.api.BobbuyApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BobbuyApplication.class)
@ActiveProfiles("test")
@Disabled("Manual integration test requiring local sample files and external AI dependencies")
public class AiVisionIntegrationManualTest {

    @Autowired
    private AiProductOnboardingService onboardingService;

    @Test
    public void testImg1484Seafood() throws Exception {
        String base64 = new String(Files.readAllBytes(Paths.get("c:/workspace/bobbuy/sample/IMG_1484.b64"))).trim();
        Optional<AiOnboardingSuggestion> suggestionOpt = onboardingService.onboardFromPhoto(base64);
        
        assertTrue(suggestionOpt.isPresent(), "Should extract info from IMG_1484");
        AiOnboardingSuggestion suggestion = suggestionOpt.get();
        
        System.out.println("--- IMG_1484 Extraction Result ---");
        System.out.println("Name: " + suggestion.name());
        System.out.println("Item Number: " + suggestion.itemNumber());
        System.out.println("Price: " + suggestion.price());
        System.out.println("Tiers: " + suggestion.detectedPriceTiers());
        
        // Assertions based on manual photo inspection
        assertEquals("53432", suggestion.itemNumber(), "Item number should match top of label");
    }

    @Test
    public void testImg1638MuffinIncremental() throws Exception {
        // Step 1: First scan for IMG_1638 (Item 93963)
        // Note: This relies on the DB being empty or not having 93963
        byte[] bytes = Files.readAllBytes(Paths.get("c:/workspace/bobbuy/sample/IMG_1638.jpg"));
        String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
        
        Optional<AiOnboardingSuggestion> firstScan = onboardingService.onboardFromPhoto(base64);
        assertTrue(firstScan.isPresent());
        System.out.println("First Scan Item Number: " + firstScan.get().itemNumber());
        
        // Simulate "Confirm" to save to DB (done manually or by calling store in full test)
    }
}
