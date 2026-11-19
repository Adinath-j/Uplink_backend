package com.example.Uplink_Backend.modules.messaging.controller;

import com.example.Uplink_Backend.common.dto.ApiResponse;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.modules.messaging.dto.ConversationDto;
import com.example.Uplink_Backend.modules.messaging.dto.CreateConversationRequest;
import com.example.Uplink_Backend.modules.messaging.dto.SendMessageRequest;
import com.example.Uplink_Backend.modules.messaging.dto.UserSearchResultDto;
import com.example.Uplink_Backend.modules.messaging.entity.Message;
import com.example.Uplink_Backend.modules.messaging.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/conversations", "/api/messages"})
@Tag(name = "8. Messaging Module", description = "Endpoints for Direct & Group Conversations, Messaging, Links, and Read Receipts")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping({"", "/conversations"})
    @Operation(summary = "Start or get existing direct conversation, or start a group conversation")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CreateConversationRequest request) {
        ConversationDto conversation = messagingService.getOrCreateConversation(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Conversation ready", conversation));
    }

    @GetMapping({"", "/conversations"})
    @Operation(summary = "Get all conversations for current user with unread counts and latest messages")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getMyConversations(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ConversationDto> conversations = messagingService.getUserConversationDtos(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok("User conversations retrieved", conversations));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation details by ID")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID conversationId) {
        ConversationDto conversation = messagingService.getConversationDto(conversationId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Conversation details retrieved", conversation));
    }

    @PostMapping({"/send", "/conversations/send"})
    @Operation(summary = "Send a text message, attachment link, or system notification in a conversation")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SendMessageRequest request) {
        Message message = messagingService.sendMessage(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.created("Message sent", message));
    }

    @GetMapping({"/conversation/{conversationId}", "/{conversationId}/messages"})
    @Operation(summary = "Get conversation message thread (paginated)")
    public ResponseEntity<ApiResponse<Page<Message>>> getMessages(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID conversationId,
            Pageable pageable) {
        Page<Message> messages = messagingService.getConversationMessages(conversationId, userPrincipal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Messages retrieved", messages));
    }

    @PostMapping({"/conversation/{conversationId}/read", "/{conversationId}/read"})
    @Operation(summary = "Mark all unread messages in conversation as read (Read receipt)")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID conversationId) {
        messagingService.markConversationAsRead(conversationId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Conversation marked as read", null));
    }

    @GetMapping("/users/search")
    @Operation(summary = "Search users/peers to start a new chat")
    public ResponseEntity<ApiResponse<List<UserSearchResultDto>>> searchPeers(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(name = "query", defaultValue = "") String query) {
        List<UserSearchResultDto> peers = messagingService.searchPeers(userPrincipal.getId(), query);
        return ResponseEntity.ok(ApiResponse.ok("Peer search results", peers));
    }
}
