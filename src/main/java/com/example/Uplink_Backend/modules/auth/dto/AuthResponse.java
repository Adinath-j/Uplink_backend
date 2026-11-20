package com.example.Uplink_Backend.modules.auth.dto;

import java.util.List;
import java.util.UUID;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private UUID id;
    private String email;
    private String fullName;
    private List<String> roles;
    private Boolean requiresVerification;
    private Boolean isVerified;

    public AuthResponse() {}

    public AuthResponse(String token, String refreshToken, UUID id, String email, String fullName, List<String> roles) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
        this.isVerified = true;
        this.requiresVerification = false;
    }

    public AuthResponse(String token, String refreshToken, UUID id, String email, String fullName, List<String> roles, Boolean isVerified) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
        this.isVerified = isVerified;
        this.requiresVerification = Boolean.FALSE.equals(isVerified);
    }

    public static AuthResponse verificationRequired(String email, String fullName, List<String> roles) {
        AuthResponse res = new AuthResponse();
        res.setEmail(email);
        res.setFullName(fullName);
        res.setRoles(roles);
        res.setToken(null);
        res.setRefreshToken(null);
        res.setRequiresVerification(true);
        res.setIsVerified(false);
        return res;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAccessToken() {
        return token;
    }

    public void setAccessToken(String accessToken) {
        this.token = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Boolean getRequiresVerification() {
        return requiresVerification;
    }

    public void setRequiresVerification(Boolean requiresVerification) {
        this.requiresVerification = requiresVerification;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }
}
