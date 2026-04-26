package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;

import java.util.List;

public record ChatConversationSlice(List<ChatMessage> messages, Long nextCursor, boolean hasMore) {
}
