package com.example.Uplink_Backend.modules.messaging.dto;

import java.util.UUID;

public class UserSearchResultDto {
    private UUID id;
    private String fullName;
    private String email;
    private String role;

    public UserSearchResultDto() {}

    public UserSearchResultDto(UUID id, String fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }

    public UserSearchResultDto(UUID id, String fullName, String email, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
