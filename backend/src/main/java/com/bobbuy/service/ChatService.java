package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;
import com.bobbuy.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
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
}
