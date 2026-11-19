package com.example.Uplink_Backend.modules.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public class ReadReceiptNotification {
    private UUID conversationId;
    private UUID userId;
    private Instant readAt;

    public ReadReceiptNotification() {}

    public ReadReceiptNotification(UUID conversationId, UUID userId, Instant readAt) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.readAt = readAt;
    }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
