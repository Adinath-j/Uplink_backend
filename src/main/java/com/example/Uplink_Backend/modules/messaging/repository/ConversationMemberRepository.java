package com.example.Uplink_Backend.modules.messaging.repository;

import com.example.Uplink_Backend.modules.messaging.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    @Query("SELECT cm FROM ConversationMember cm WHERE cm.conversation.id = :conversationId")
    List<ConversationMember> findByConversationId(@Param("conversationId") UUID conversationId);

    @Query("SELECT cm FROM ConversationMember cm WHERE cm.conversation.id = :conversationId AND cm.user.id = :userId")
    Optional<ConversationMember> findByConversationIdAndUserId(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END FROM ConversationMember cm WHERE cm.conversation.id = :conversationId AND cm.user.id = :userId")
    boolean existsByConversationIdAndUserId(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
