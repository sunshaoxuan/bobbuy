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
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        assertEquals("prd-1", ((Map<?, ?>) saved.getMetadata().get("productSnapshot")).get("productId"));
        assertEquals("抹茶礼盒", ((Map<?, ?>) saved.getMetadata().get("productSnapshot")).get("summary"));
        verify(messagingTemplate).convertAndSend(eq("/topic/trip/2000"), eq(saved));
        assertPersistedMessageCount(1);
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
        assertNotNull(saved.getMetadata().get("clientMessageId"));
        verify(messagingTemplate).convertAndSend(eq("/topic/private/AAA/ZZZ"), eq(saved));
        assertPersistedMessageCount(1);
    }

    @Test
    void getTripConversationSliceUsesIdCursorPagination() {
        LongStream.rangeClosed(1, 4).forEach(index -> {
            ChatMessage message = new ChatMessage();
            message.setTripId(3000L);
            message.setSenderId("PURCHASER");
            message.setRecipientId("DEMO-CUST");
            message.setContent("message-" + index);
            message.setType("TEXT");
            chatMessageRepository.save(message);
        });

        ChatConversationSlice firstSlice = chatService.getTripConversationSlice(3000L, null, 2);

        assertEquals(2, firstSlice.messages().size());
        assertTrue(firstSlice.hasMore());
        assertEquals("message-3", firstSlice.messages().get(0).getContent());
        assertEquals("message-4", firstSlice.messages().get(1).getContent());

        ChatConversationSlice secondSlice = chatService.getTripConversationSlice(3000L, firstSlice.nextCursor(), 2);

        assertEquals(2, secondSlice.messages().size());
        assertEquals("message-1", secondSlice.messages().get(0).getContent());
        assertEquals("message-2", secondSlice.messages().get(1).getContent());
        assertFalse(secondSlice.hasMore());
    }

    private void assertPersistedMessageCount(int expectedCount) {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (chatMessageRepository.count() == expectedCount) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertEquals(expectedCount, chatMessageRepository.count());
    }
}
