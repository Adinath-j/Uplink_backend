package com.example.Uplink_Backend.modules.messaging.repository;

import com.example.Uplink_Backend.modules.messaging.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    @Query("SELECT cm.conversation FROM ConversationMember cm WHERE cm.user.id = :userId ORDER BY cm.conversation.updatedAt DESC")
    List<Conversation> findConversationsByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Conversation c WHERE c.type = 'DIRECT' " +
           "AND EXISTS (SELECT 1 FROM ConversationMember cm1 WHERE cm1.conversation = c AND cm1.user.id = :userId1) " +
           "AND EXISTS (SELECT 1 FROM ConversationMember cm2 WHERE cm2.conversation = c AND cm2.user.id = :userId2) " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findDirectConversationBetweenUsers(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
