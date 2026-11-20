package com.example.Uplink_Backend.common.exception;

public class EmailNotVerifiedException extends RuntimeException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("Email verification required. A 6-digit code has been sent to your email.");
        this.email = email;
    }

    public EmailNotVerifiedException(String email, String message) {
        super(message != null ? message : "Email verification required. A 6-digit code has been sent to your email.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
