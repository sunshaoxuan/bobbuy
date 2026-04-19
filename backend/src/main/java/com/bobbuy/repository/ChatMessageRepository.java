package com.bobbuy.repository;

import com.bobbuy.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    @Query("SELECT c FROM ChatMessage c WHERE (c.senderId = :userA AND c.recipientId = :userB) OR (c.senderId = :userB AND c.recipientId = :userA) ORDER BY c.createdAt ASC")
    List<ChatMessage> findConversation(@Param("userA") String userA, @Param("userB") String userB);
}
