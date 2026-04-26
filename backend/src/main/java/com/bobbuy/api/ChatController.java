package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.ChatMessage;
import com.bobbuy.service.ChatConversationSlice;
import com.bobbuy.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<ChatMessage>> sendMessage(@RequestBody ChatMessage message) {
        return ResponseEntity.ok(ApiResponse.success(chatService.sendMessage(message)));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getOrderChat(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getOrderConversation(orderId)));
    }

    @GetMapping("/orders/{orderId}/cursor")
    public ResponseEntity<ApiResponse<ChatConversationSlice>> getOrderChatCursor(
        @PathVariable Long orderId,
        @RequestParam(required = false) Long beforeId,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getOrderConversationSlice(orderId, beforeId, limit)));
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getTripChat(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getTripConversation(tripId)));
    }

    @GetMapping("/trips/{tripId}/cursor")
    public ResponseEntity<ApiResponse<ChatConversationSlice>> getTripChatCursor(
        @PathVariable Long tripId,
        @RequestParam(required = false) Long beforeId,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getTripConversationSlice(tripId, beforeId, limit)));
    }

    @GetMapping("/private")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getPrivateChat(@RequestParam String userA, @RequestParam String userB) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getPrivateConversation(userA, userB)));
    }

    @GetMapping("/private/cursor")
    public ResponseEntity<ApiResponse<ChatConversationSlice>> getPrivateChatCursor(
        @RequestParam String userA,
        @RequestParam String userB,
        @RequestParam(required = false) Long beforeId,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getPrivateConversationSlice(userA, userB, beforeId, limit)));
    }
}
