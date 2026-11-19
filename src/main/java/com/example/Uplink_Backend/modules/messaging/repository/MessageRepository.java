package com.example.Uplink_Backend.modules.messaging.repository;

import com.example.Uplink_Backend.modules.messaging.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC")
    Page<Message> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findRecentMessagesByConversationId(@Param("conversationId") UUID conversationId, Pageable pageable);

    default Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId) {
        List<Message> list = findRecentMessagesByConversationId(conversationId, PageRequest.of(0, 1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND (m.isRead = false OR m.isRead IS NULL)")
    long countUnreadMessages(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = :readAt WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND (m.isRead = false OR m.isRead IS NULL)")
    void markAllAsRead(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
