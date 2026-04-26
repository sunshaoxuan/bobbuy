package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;
import com.bobbuy.repository.ChatMessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private static final DateTimeFormatter ISO_LOCAL_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatMessageRepository chatMessageRepository, SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
        message.setMetadata(normalizeMetadata(message));
        ChatMessage savedMessage = chatMessageRepository.save(message);
        messagingTemplate.convertAndSend(resolveDestination(savedMessage), savedMessage);
        return savedMessage;
    }

    public List<ChatMessage> getOrderConversation(Long orderId) {
        return chatMessageRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    public List<ChatMessage> getTripConversation(Long tripId) {
        return chatMessageRepository.findByTripIdOrderByCreatedAtAsc(tripId);
    }

    public List<ChatMessage> getPrivateConversation(String userA, String userB) {
        return chatMessageRepository.findConversation(userA, userB);
    }

    private Map<String, Object> normalizeMetadata(ChatMessage message) {
        Map<String, Object> metadata = message.getMetadata() == null
            ? new HashMap<>()
            : new HashMap<>(message.getMetadata());
        metadata.putIfAbsent("source", "CHAT_WIDGET");
        metadata.putIfAbsent("auditVersion", "V14");
        metadata.putIfAbsent("operatorId", message.getSenderId());
        metadata.putIfAbsent("conversationType", resolveConversationType(message));
        metadata.putIfAbsent("orderId", message.getOrderId());
        metadata.putIfAbsent("tripId", message.getTripId());
        metadata.putIfAbsent("relatedOrderId", message.getOrderId());
        metadata.putIfAbsent("relatedTripId", message.getTripId());
        if ("IMAGE".equalsIgnoreCase(message.getType())) {
            Object primaryUrl = metadata.get("attachmentUrl");
            if (primaryUrl == null && metadata.get("url") != null) {
                metadata.put("attachmentUrl", metadata.get("url"));
            }
            metadata.putIfAbsent("imageFlowStatus", "PENDING_CONFIRMATION");
            if (metadata.get("attachmentUrl") == null) {
                metadata.putIfAbsent("recoveryAction", "REQUEST_ATTACHMENT_REUPLOAD");
            }
            if ("PUBLISH_FAILED".equals(metadata.get("imageFlowStatus"))) {
                metadata.putIfAbsent("recoveryAction", "RETRY_PUBLISH");
            }
            if ("PUBLISHED_TO_MARKET".equals(metadata.get("imageFlowStatus"))) {
                metadata.putIfAbsent("publishedAt", ISO_LOCAL_DATETIME.format(message.getCreatedAt()));
            }
        }
        return metadata;
    }

    private String resolveConversationType(ChatMessage message) {
        if (message.getTripId() != null) {
            return "TRIP";
        }
        if (message.getOrderId() != null) {
            return "ORDER";
        }
        return "PRIVATE";
    }

    private String resolveDestination(ChatMessage message) {
        String conversationType = resolveConversationType(message);
        if ("TRIP".equals(conversationType) && message.getTripId() != null) {
            return "/topic/trip/" + message.getTripId();
        }
        if ("ORDER".equals(conversationType) && message.getOrderId() != null) {
            return "/topic/order/" + message.getOrderId();
        }
        String senderId = message.getSenderId();
        String recipientId = message.getRecipientId();
        if (senderId.compareTo(recipientId) > 0) {
            String temp = senderId;
            senderId = recipientId;
            recipientId = temp;
        }
        return "/topic/private/"
            + UriUtils.encodePathSegment(senderId, StandardCharsets.UTF_8)
            + "/"
            + UriUtils.encodePathSegment(recipientId, StandardCharsets.UTF_8);
    }
}
