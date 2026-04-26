package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;
import com.bobbuy.repository.ChatMessageRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class ChatPersistenceWorker {
    private final ChatMessageRepository chatMessageRepository;

    public ChatPersistenceWorker(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Async
    public void persistAsync(ChatMessage message) {
        chatMessageRepository.save(copyOf(message));
    }

    private ChatMessage copyOf(ChatMessage message) {
        ChatMessage copy = new ChatMessage();
        copy.setId(message.getId());
        copy.setOrderId(message.getOrderId());
        copy.setTripId(message.getTripId());
        copy.setSenderId(message.getSenderId());
        copy.setRecipientId(message.getRecipientId());
        copy.setContent(message.getContent());
        copy.setType(message.getType());
        copy.setCreatedAt(message.getCreatedAt());
        copy.setMetadata(message.getMetadata() == null ? null : new HashMap<>(message.getMetadata()));
        return copy;
    }
}
