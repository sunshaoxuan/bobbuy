package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;
import com.bobbuy.repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatService {
    private static final int DEFAULT_CURSOR_LIMIT = 50;
    private static final int MAX_CURSOR_LIMIT = 100;
    private static final DateTimeFormatter ISO_LOCAL_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatDestinationResolver destinationResolver;
    private final ChatRealtimePublisher chatRealtimePublisher;
    private final ChatPersistenceWorker chatPersistenceWorker;

    public ChatService(
        ChatMessageRepository chatMessageRepository,
        ChatDestinationResolver destinationResolver,
        ChatRealtimePublisher chatRealtimePublisher,
        ChatPersistenceWorker chatPersistenceWorker
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.destinationResolver = destinationResolver;
        this.chatRealtimePublisher = chatRealtimePublisher;
        this.chatPersistenceWorker = chatPersistenceWorker;
    }

    public ChatMessage sendMessage(ChatMessage message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        message.setMetadata(normalizeMetadata(message));
        chatRealtimePublisher.publish(message);
        chatPersistenceWorker.persistAsync(message);
        return message;
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

    public ChatConversationSlice getOrderConversationSlice(Long orderId, Long beforeId, Integer limit) {
        return buildSlice(beforeId, limit,
            normalizedLimit -> beforeId == null
                ? chatMessageRepository.findByOrderIdOrderByIdDesc(orderId, PageRequest.of(0, normalizedLimit + 1))
                : chatMessageRepository.findByOrderIdAndIdLessThanOrderByIdDesc(orderId, beforeId, PageRequest.of(0, normalizedLimit + 1))
        );
    }

    public ChatConversationSlice getTripConversationSlice(Long tripId, Long beforeId, Integer limit) {
        return buildSlice(beforeId, limit,
            normalizedLimit -> beforeId == null
                ? chatMessageRepository.findByTripIdOrderByIdDesc(tripId, PageRequest.of(0, normalizedLimit + 1))
                : chatMessageRepository.findByTripIdAndIdLessThanOrderByIdDesc(tripId, beforeId, PageRequest.of(0, normalizedLimit + 1))
        );
    }

    public ChatConversationSlice getPrivateConversationSlice(String userA, String userB, Long beforeId, Integer limit) {
        return buildSlice(beforeId, limit,
            normalizedLimit -> beforeId == null
                ? chatMessageRepository.findConversationPage(userA, userB, PageRequest.of(0, normalizedLimit + 1))
                : chatMessageRepository.findConversationPageBefore(userA, userB, beforeId, PageRequest.of(0, normalizedLimit + 1))
        );
    }

    private Map<String, Object> normalizeMetadata(ChatMessage message) {
        Map<String, Object> metadata = message.getMetadata() == null
            ? new HashMap<>()
            : new HashMap<>(message.getMetadata());
        metadata.putIfAbsent("source", "CHAT_WIDGET");
        metadata.putIfAbsent("auditVersion", "V14");
        metadata.putIfAbsent("operatorId", message.getSenderId());
        metadata.putIfAbsent("conversationType", destinationResolver.resolveConversationType(message));
        metadata.putIfAbsent("orderId", message.getOrderId());
        metadata.putIfAbsent("tripId", message.getTripId());
        metadata.putIfAbsent("relatedOrderId", message.getOrderId());
        metadata.putIfAbsent("relatedTripId", message.getTripId());
        metadata.putIfAbsent("clientMessageId", UUID.randomUUID().toString());
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
        addProductSnapshot(metadata, message);
        return metadata;
    }

    private void addProductSnapshot(Map<String, Object> metadata, ChatMessage message) {
        Object productId = metadata.get("productId");
        Object productName = metadata.get("productName");
        Object itemNumber = metadata.get("itemNumber");
        Object visibilityStatus = metadata.get("visibilityStatus");
        if (productId == null && productName == null && itemNumber == null) {
            return;
        }
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("productId", productId);
        snapshot.put("productName", productName == null ? message.getContent() : productName);
        snapshot.put("itemNumber", itemNumber);
        snapshot.put("visibilityStatus", visibilityStatus);
        snapshot.put("summary", buildProductSummary(productName == null ? message.getContent() : String.valueOf(productName), itemNumber, visibilityStatus));
        metadata.putIfAbsent("productSnapshot", snapshot);
    }

    private String buildProductSummary(String productName, Object itemNumber, Object visibilityStatus) {
        List<String> parts = new ArrayList<>();
        if (productName != null && !productName.isBlank()) {
            parts.add(productName);
        }
        if (itemNumber != null) {
            parts.add("SKU " + itemNumber);
        }
        if (visibilityStatus != null) {
            parts.add(String.valueOf(visibilityStatus));
        }
        return String.join(" · ", parts);
    }

    private ChatConversationSlice buildSlice(Long beforeId, Integer limit, CursorQuery cursorQuery) {
        int normalizedLimit = normalizeLimit(limit);
        List<ChatMessage> descendingMessages = cursorQuery.fetch(normalizedLimit);
        boolean hasMore = descendingMessages.size() > normalizedLimit;
        List<ChatMessage> page = hasMore
            ? new ArrayList<>(descendingMessages.subList(0, normalizedLimit))
            : new ArrayList<>(descendingMessages);
        Collections.reverse(page);
        Long nextCursor = hasMore && !page.isEmpty() ? page.get(0).getId() : null;
        return new ChatConversationSlice(page, nextCursor, hasMore);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_CURSOR_LIMIT;
        }
        return Math.min(limit, MAX_CURSOR_LIMIT);
    }

    @FunctionalInterface
    private interface CursorQuery {
        List<ChatMessage> fetch(int normalizedLimit);
    }
}
