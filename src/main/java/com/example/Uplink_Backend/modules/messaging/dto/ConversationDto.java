package com.example.Uplink_Backend.modules.messaging.dto;

import com.example.Uplink_Backend.modules.messaging.entity.Message;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConversationDto {

    private UUID id;
    private String type;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MemberSummary> members = new ArrayList<>();
    private Message lastMessage;
    private long unreadCount;

    public ConversationDto() {}

    public static class MemberSummary {
        private UUID id;
        private UserSummary user;
        private String role;
        private Instant joinedAt;

        public MemberSummary() {}

        public MemberSummary(UUID id, UserSummary user, String role, Instant joinedAt) {
            this.id = id;
            this.user = user;
            this.role = role;
            this.joinedAt = joinedAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UserSummary getUser() { return user; }
        public void setUser(UserSummary user) { this.user = user; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Instant getJoinedAt() { return joinedAt; }
        public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    }

    public static class UserSummary {
        private UUID id;
        private String fullName;
        private String email;

        public UserSummary() {}

        public UserSummary(UUID id, String fullName, String email) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<MemberSummary> getMembers() { return members; }
    public void setMembers(List<MemberSummary> members) { this.members = members; }
    public Message getLastMessage() { return lastMessage; }
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }
    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
}
