package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;

public record ChatRealtimeEnvelope(String destination, ChatMessage message) {
}
