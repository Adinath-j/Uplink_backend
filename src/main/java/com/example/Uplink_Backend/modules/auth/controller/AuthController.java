package com.example.Uplink_Backend.modules.auth.controller;

import com.example.Uplink_Backend.common.dto.ApiResponse;
import com.example.Uplink_Backend.common.exception.BadRequestException;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.modules.auth.dto.*;
import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Auth Module", description = "Endpoints for user registration, login, JWT token refresh, email verification, and password reset")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account (Student, Recruiter, Admin, etc.)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        String message = Boolean.TRUE.equals(response.getRequiresVerification())
                ? "Registration initiated. A 6-digit verification code has been dispatched to " + response.getEmail()
                : "User registered successfully";
        HttpStatus status = Boolean.TRUE.equals(response.getRequiresVerification()) ? HttpStatus.OK : HttpStatus.CREATED;
        return new ResponseEntity<>(ApiResponse.ok(message, response), status);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and generate JWT access & refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify account email address using 6-digit OTP")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", response));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend 6-digit verification code with 60-second cooldown")
    public ResponseEntity<ApiResponse<String>> resendVerification(
            @RequestBody(required = false) ResendVerificationRequest body,
            @RequestParam(required = false) String email) {
        String targetEmail = null;
        if (body != null && body.getEmail() != null && !body.getEmail().isBlank()) {
            targetEmail = body.getEmail();
        } else if (email != null && !email.isBlank()) {
            targetEmail = email;
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            throw new BadRequestException("Email address is required to resend verification code");
        }

        authService.resendVerification(targetEmail);
        return ResponseEntity.ok(ApiResponse.ok("Verification code sent to " + targetEmail, null));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Generate a new access token using a valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
        authService.requestPasswordReset(email);
        return ResponseEntity.ok(ApiResponse.ok("Password reset instructions sent to your email", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password has been successfully reset", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user details")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Current user profile retrieved", user));
    }
}
