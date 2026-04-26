package com.bobbuy.repository;

import com.bobbuy.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    List<ChatMessage> findByTripIdOrderByCreatedAtAsc(Long tripId);
    List<ChatMessage> findByOrderIdOrderByIdDesc(Long orderId, Pageable pageable);
    List<ChatMessage> findByTripIdOrderByIdDesc(Long tripId, Pageable pageable);
    List<ChatMessage> findByOrderIdAndIdLessThanOrderByIdDesc(Long orderId, Long beforeId, Pageable pageable);
    List<ChatMessage> findByTripIdAndIdLessThanOrderByIdDesc(Long tripId, Long beforeId, Pageable pageable);

    @Query("SELECT c FROM ChatMessage c WHERE (c.senderId = :userA AND c.recipientId = :userB) OR (c.senderId = :userB AND c.recipientId = :userA) ORDER BY c.createdAt ASC")
    List<ChatMessage> findConversation(@Param("userA") String userA, @Param("userB") String userB);

    @Query("""
        SELECT c FROM ChatMessage c
        WHERE ((c.senderId = :userA AND c.recipientId = :userB) OR (c.senderId = :userB AND c.recipientId = :userA))
        ORDER BY c.id DESC
        """)
    List<ChatMessage> findConversationPage(@Param("userA") String userA, @Param("userB") String userB, Pageable pageable);

    @Query("""
        SELECT c FROM ChatMessage c
        WHERE c.id < :beforeId
          AND ((c.senderId = :userA AND c.recipientId = :userB) OR (c.senderId = :userB AND c.recipientId = :userA))
        ORDER BY c.id DESC
        """)
    List<ChatMessage> findConversationPageBefore(
        @Param("userA") String userA,
        @Param("userB") String userB,
        @Param("beforeId") Long beforeId,
        Pageable pageable
    );
}
