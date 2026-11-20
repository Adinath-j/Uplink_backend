package com.example.Uplink_Backend.modules.messaging.service;

import com.example.Uplink_Backend.common.exception.BadRequestException;
import com.example.Uplink_Backend.common.exception.ResourceNotFoundException;
import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.repository.UserRepository;
import com.example.Uplink_Backend.modules.messaging.dto.*;
import com.example.Uplink_Backend.modules.messaging.entity.Conversation;
import com.example.Uplink_Backend.modules.messaging.entity.ConversationMember;
import com.example.Uplink_Backend.modules.messaging.entity.Message;
import com.example.Uplink_Backend.modules.messaging.repository.ConversationMemberRepository;
import com.example.Uplink_Backend.modules.messaging.repository.ConversationRepository;
import com.example.Uplink_Backend.modules.messaging.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessagingService(ConversationRepository conversationRepository,
                            ConversationMemberRepository memberRepository,
                            MessageRepository messageRepository,
                            UserRepository userRepository,
                            SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Conversation createConversation(UUID creatorId, CreateConversationRequest request) {
        return createOrGetConversationEntity(creatorId, request);
    }

    @Transactional
    public ConversationDto getOrCreateConversation(UUID creatorId, CreateConversationRequest request) {
        Conversation conversation = createOrGetConversationEntity(creatorId, request);
        return toConversationDto(conversation, creatorId);
    }

    private Conversation createOrGetConversationEntity(UUID creatorId, CreateConversationRequest request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        List<UUID> participants = request.getParticipantIds();
        if (participants == null || participants.isEmpty()) {
            throw new BadRequestException("At least one recipient/participant is required to start a conversation");
        }

        boolean isDirect = "DIRECT".equalsIgnoreCase(request.getType()) || participants.size() == 1;

        // Idempotent 1-on-1 check
        if (isDirect && participants.size() == 1) {
            UUID recipientId = participants.get(0);
            if (recipientId.equals(creatorId)) {
                throw new BadRequestException("You cannot start a direct conversation with yourself");
            }

            List<Conversation> existing = conversationRepository.findDirectConversationBetweenUsers(creatorId, recipientId);
            if (!existing.isEmpty()) {
                return existing.get(0);
            }
        }

        Conversation conversation = new Conversation(isDirect ? "DIRECT" : "GROUP");
        Conversation savedConversation = conversationRepository.save(conversation);

        // Add creator
        ConversationMember creatorMember = new ConversationMember(savedConversation, creator, "ADMIN");
        memberRepository.save(creatorMember);
        savedConversation.getMembers().add(creatorMember);

        Set<UUID> addedIds = new HashSet<>();
        addedIds.add(creatorId);

        for (UUID participantId : participants) {
            if (!addedIds.contains(participantId)) {
                User participant = userRepository.findById(participantId).orElse(null);
                if (participant != null) {
                    ConversationMember member = new ConversationMember(savedConversation, participant, "MEMBER");
                    memberRepository.save(member);
                    savedConversation.getMembers().add(member);
                    addedIds.add(participantId);
                }
            }
        }

        return savedConversation;
    }

    public List<Conversation> getUserConversations(UUID userId) {
        return conversationRepository.findConversationsByUserId(userId);
    }

    public List<ConversationDto> getUserConversationDtos(UUID userId) {
        List<Conversation> conversations = conversationRepository.findConversationsByUserIdWithMembers(userId);
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> convIds = conversations.stream().map(Conversation::getId).toList();

        // 1. Batch unread counts for all conversations in 1 query
        Map<UUID, Long> unreadMap = new HashMap<>();
        try {
            List<Object[]> unreadRows = messageRepository.countUnreadByConversationIds(convIds, userId);
            for (Object[] row : unreadRows) {
                if (row != null && row.length >= 2 && row[0] instanceof UUID cId && row[1] instanceof Number count) {
                    unreadMap.put(cId, count.longValue());
                }
            }
        } catch (Exception e) {
            // Fallback to per-conversation count if batch fails
            for (Conversation c : conversations) {
                unreadMap.put(c.getId(), messageRepository.countUnreadMessages(c.getId(), userId));
            }
        }

        // 2. Batch latest messages for all conversations in 1 query
        Map<UUID, Message> latestMessageMap = new HashMap<>();
        try {
            List<Message> latestMessages = messageRepository.findLatestMessagesByConversationIds(convIds);
            for (Message msg : latestMessages) {
                if (msg != null && msg.getConversation() != null) {
                    UUID cId = msg.getConversation().getId();
                    Message existing = latestMessageMap.get(cId);
                    if (existing == null || (msg.getCreatedAt() != null && existing.getCreatedAt() != null && msg.getCreatedAt().isAfter(existing.getCreatedAt()))) {
                        latestMessageMap.put(cId, msg);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to per-conversation lookup if batch query fails
            for (Conversation c : conversations) {
                messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(c.getId())
                        .ifPresent(m -> latestMessageMap.put(c.getId(), m));
            }
        }

        return conversations.stream()
                .map(c -> toConversationDto(c, userId, latestMessageMap.get(c.getId()), unreadMap.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public ConversationDto getConversationDto(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new BadRequestException("You are not a member of this conversation");
        }
        return toConversationDto(conversation, userId);
    }

    public ConversationDto toConversationDto(Conversation conversation, UUID currentUserId) {
        Optional<Message> lastMessageOpt = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId());
        long unread = currentUserId != null ? messageRepository.countUnreadMessages(conversation.getId(), currentUserId) : 0L;
        return toConversationDto(conversation, currentUserId, lastMessageOpt.orElse(null), unread);
    }

    public ConversationDto toConversationDto(Conversation conversation, UUID currentUserId, Message lastMessage, Long unreadCount) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setType(conversation.getType());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        List<ConversationMember> members = conversation.getMembers();
        if (members == null || members.isEmpty()) {
            members = memberRepository.findByConversationId(conversation.getId());
        }

        List<ConversationDto.MemberSummary> memberSummaries = new ArrayList<>();
        if (members != null) {
            for (ConversationMember m : members) {
                User u = m.getUser();
                ConversationDto.UserSummary userSummary = u != null
                        ? new ConversationDto.UserSummary(u.getId(), u.getFullName(), u.getEmail())
                        : null;
                memberSummaries.add(new ConversationDto.MemberSummary(m.getId(), userSummary, m.getRole(), m.getJoinedAt()));
            }
        }
        dto.setMembers(memberSummaries);

        if (lastMessage != null) {
            dto.setLastMessage(lastMessage);
        }

        dto.setUnreadCount(unreadCount != null ? unreadCount : 0L);

        return dto;
    }

    @Transactional
    public Message sendMessage(UUID senderId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", request.getConversationId()));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        // Ensure sender is a conversation member
        if (!memberRepository.existsByConversationIdAndUserId(conversation.getId(), senderId)) {
            throw new BadRequestException("You are not a member of this conversation");
        }

        Message message = new Message(conversation, sender, request.getContent(), request.getMessageType());
        if (request.getClientMessageId() != null) {
            message.setClientMessageId(request.getClientMessageId());
        }
        Message savedMessage = messageRepository.save(message);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // Real-time WebSocket delivery
        try {
            // Broadcast to the conversation room
            messagingTemplate.convertAndSend("/topic/conversations." + conversation.getId(), savedMessage);

            // Broadcast to recipients' personal channels for in-app alert toasts & inbox sync
            List<ConversationMember> members = memberRepository.findByConversationId(conversation.getId());
            for (ConversationMember member : members) {
                if (member.getUser() != null && !member.getUser().getId().equals(senderId)) {
                    messagingTemplate.convertAndSend("/topic/user." + member.getUser().getId() + ".messages", savedMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to broadcast message via WebSocket: " + e.getMessage());
        }

        return savedMessage;
    }

    public Page<Message> getConversationMessages(UUID conversationId, UUID userId, Pageable pageable) {
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new BadRequestException("You are not a member of this conversation");
        }
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
    }

    @Transactional
    public void markConversationAsRead(UUID conversationId, UUID userId) {
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new BadRequestException("You are not a member of this conversation");
        }
        Instant readAt = Instant.now();
        messageRepository.markAllAsRead(conversationId, userId, readAt);

        // Broadcast read receipt to room
        try {
            ReadReceiptNotification receipt = new ReadReceiptNotification(conversationId, userId, readAt);
            messagingTemplate.convertAndSend("/topic/conversations." + conversationId + ".read", receipt);
        } catch (Exception e) {
            System.err.println("Failed to broadcast read receipt via WebSocket: " + e.getMessage());
        }
    }

    public void broadcastTyping(UUID conversationId, UUID userId, String userName, boolean isTyping) {
        try {
            TypingNotification notif = new TypingNotification(conversationId, userId, userName, isTyping);
            messagingTemplate.convertAndSend("/topic/conversations." + conversationId + ".typing", notif);
        } catch (Exception e) {
            System.err.println("Failed to broadcast typing via WebSocket: " + e.getMessage());
        }
    }

    public List<UserSearchResultDto> searchPeers(UUID currentUserId, String query) {
        List<User> users;
        if (query == null || query.trim().isEmpty()) {
            users = userRepository.findSuggestedUsers(currentUserId, PageRequest.of(0, 30));
        } else {
            String q = query.trim();
            users = userRepository.searchUsers(q, currentUserId, PageRequest.of(0, 30));
            // If no match found, try collapsed repeating characters (e.g. "abhii" -> "abhi")
            if (users.isEmpty() && q.length() >= 3) {
                String collapsed = q.replaceAll("(.)\\1+", "$1");
                if (!collapsed.equalsIgnoreCase(q)) {
                    users = userRepository.searchUsers(collapsed, currentUserId, PageRequest.of(0, 30));
                }
            }
            // If still no match and query length >= 4, try prefix matching
            if (users.isEmpty() && q.length() >= 4) {
                String prefix = q.substring(0, 3);
                users = userRepository.searchUsers(prefix, currentUserId, PageRequest.of(0, 30));
            }
        }
        return users.stream()
                .map(u -> {
                    String roleName = null;
                    if (u.getRoles() != null && !u.getRoles().isEmpty()) {
                        roleName = u.getRoles().iterator().next().getName();
                        if (roleName != null && roleName.startsWith("ROLE_")) {
                            roleName = roleName.substring(5);
                        }
                    }
                    return new UserSearchResultDto(u.getId(), u.getFullName(), u.getEmail(), roleName);
                })
                .collect(Collectors.toList());
    }
}
