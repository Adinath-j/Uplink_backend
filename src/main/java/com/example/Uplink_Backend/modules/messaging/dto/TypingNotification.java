package com.example.Uplink_Backend.modules.messaging.dto;

import java.util.UUID;

public class TypingNotification {
    private UUID conversationId;
    private UUID userId;
    private String userName;
    private boolean isTyping;

    public TypingNotification() {}

    public TypingNotification(UUID conversationId, UUID userId, String userName, boolean isTyping) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.userName = userName;
        this.isTyping = isTyping;
    }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { isTyping = typing; }
}
