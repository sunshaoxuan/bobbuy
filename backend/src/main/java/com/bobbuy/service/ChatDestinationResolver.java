package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Component
public class ChatDestinationResolver {

    public String resolve(ChatMessage message) {
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

    public String resolveConversationType(ChatMessage message) {
        if (message.getTripId() != null) {
            return "TRIP";
        }
        if (message.getOrderId() != null) {
            return "ORDER";
        }
        return "PRIVATE";
    }
}
