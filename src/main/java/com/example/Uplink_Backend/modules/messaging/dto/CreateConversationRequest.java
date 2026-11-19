package com.example.Uplink_Backend.modules.messaging.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateConversationRequest {

    private String type = "DIRECT"; // DIRECT, GROUP

    private List<UUID> participantIds;

    private UUID recipientId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<UUID> getParticipantIds() {
        if (participantIds == null) {
            participantIds = new ArrayList<>();
        }
        if (recipientId != null && !participantIds.contains(recipientId)) {
            participantIds.add(recipientId);
        }
        return participantIds;
    }

    public void setParticipantIds(List<UUID> participantIds) {
        this.participantIds = participantIds;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }
}
