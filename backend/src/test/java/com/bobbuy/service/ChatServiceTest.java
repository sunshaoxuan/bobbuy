package com.bobbuy.service;

import com.bobbuy.api.BobbuyApplication;
import com.bobbuy.model.ChatMessage;
import com.bobbuy.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = BobbuyApplication.class)
@ActiveProfiles("test")
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
    }

    @Test
    void sendMessageAddsNormalizedBusinessMetadataForImageMessages() {
        ChatMessage message = new ChatMessage();
        message.setTripId(2000L);
        message.setSenderId("PURCHASER");
        message.setRecipientId("DEMO-CUST");
        message.setContent("抹茶礼盒");
        message.setType("IMAGE");
        message.setMetadata(Map.of("url", "https://img.example/matcha.png", "productId", "prd-1"));

        ChatMessage saved = chatService.sendMessage(message);

        assertEquals("TRIP", saved.getMetadata().get("conversationType"));
        assertEquals("CHAT_WIDGET", saved.getMetadata().get("source"));
        assertEquals("V14", saved.getMetadata().get("auditVersion"));
        assertEquals("PURCHASER", saved.getMetadata().get("operatorId"));
        assertEquals(2000L, saved.getMetadata().get("tripId"));
        assertEquals(2000L, saved.getMetadata().get("relatedTripId"));
        assertEquals("https://img.example/matcha.png", saved.getMetadata().get("attachmentUrl"));
        assertEquals("PENDING_CONFIRMATION", saved.getMetadata().get("imageFlowStatus"));
    }
}
