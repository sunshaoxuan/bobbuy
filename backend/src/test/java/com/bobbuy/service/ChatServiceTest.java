package com.bobbuy.service;

import com.bobbuy.api.BobbuyApplication;
import com.bobbuy.model.ChatMessage;
import com.bobbuy.repository.ChatMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = BobbuyApplication.class)
@ActiveProfiles("test")
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        reset(messagingTemplate);
    }

    @AfterEach
    void tearDown() {
        reset(messagingTemplate);
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
        verify(messagingTemplate).convertAndSend(eq("/topic/trip/2000"), eq(saved));
    }

    @Test
    void sendMessagePublishesPrivateMessagesToStableTopic() {
        ChatMessage message = new ChatMessage();
        message.setSenderId("ZZZ");
        message.setRecipientId("AAA");
        message.setContent("hello");
        message.setType("TEXT");

        ChatMessage saved = chatService.sendMessage(message);

        assertEquals("PRIVATE", saved.getMetadata().get("conversationType"));
        verify(messagingTemplate).convertAndSend(eq("/topic/private/AAA/ZZZ"), eq(saved));
    }
}
