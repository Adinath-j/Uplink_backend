package com.example.Uplink_Backend.modules.messaging.dto;

import java.util.UUID;

public class UserSearchResultDto {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private String headline;
    private String avatarUrl;

    public UserSearchResultDto() {}

    public UserSearchResultDto(UUID id, String fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }

    public UserSearchResultDto(UUID id, String fullName, String email, String role, String headline, String avatarUrl) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.headline = headline;
        this.avatarUrl = avatarUrl;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
