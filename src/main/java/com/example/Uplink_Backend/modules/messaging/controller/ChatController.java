package com.example.Uplink_Backend.modules.messaging.controller;

import com.example.Uplink_Backend.common.exception.BadRequestException;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.modules.messaging.dto.ReadReceiptNotification;
import com.example.Uplink_Backend.modules.messaging.dto.SendMessageRequest;
import com.example.Uplink_Backend.modules.messaging.dto.TypingNotification;
import com.example.Uplink_Backend.modules.messaging.service.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final MessagingService messagingService;

    public ChatController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @MessageMapping("/chat.send")
    public void handleSendMessage(@Payload SendMessageRequest request, Principal principal) {
        UUID senderId = resolveUserId(principal);
        if (senderId == null) {
            logger.warn("[ChatController] Unauthenticated STOMP send attempt rejected");
            return;
        }
        messagingService.sendMessage(senderId, request);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingNotification notification, Principal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null || notification.getConversationId() == null) return;

        String userName = resolveUserName(principal);
        messagingService.broadcastTyping(notification.getConversationId(), userId, userName, notification.isTyping());
    }

    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload ReadReceiptNotification notification, Principal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null || notification.getConversationId() == null) return;

        messagingService.markConversationAsRead(notification.getConversationId(), userId);
    }

    private UUID resolveUserId(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof Authentication auth) {
            if (auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getId();
            }
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUserName(Principal principal) {
        if (principal instanceof Authentication auth) {
            if (auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getFullName();
            }
        }
        return "Peer";
    }
}
