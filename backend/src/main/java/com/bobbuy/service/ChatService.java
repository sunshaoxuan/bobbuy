package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;
import com.bobbuy.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
        message.setMetadata(normalizeMetadata(message));
        return chatMessageRepository.save(message);
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
}
