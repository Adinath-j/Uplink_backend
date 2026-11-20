package com.example.Uplink_Backend.modules.auth.service;

import com.example.Uplink_Backend.common.exception.BadRequestException;
import com.example.Uplink_Backend.common.exception.EmailNotVerifiedException;
import com.example.Uplink_Backend.common.exception.ResourceNotFoundException;
import com.example.Uplink_Backend.common.security.JwtTokenProvider;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.common.service.EmailService;
import com.example.Uplink_Backend.modules.auth.dto.*;
import com.example.Uplink_Backend.modules.auth.entity.EmailVerificationOtp;
import com.example.Uplink_Backend.modules.auth.entity.Role;
import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.repository.EmailVerificationOtpRepository;
import com.example.Uplink_Backend.modules.auth.repository.RoleRepository;
import com.example.Uplink_Backend.modules.auth.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = Set.of(
            "tempmail.com",
            "10minutemail.com",
            "mailinator.com",
            "guerrillamail.com",
            "yopmail.com",
            "trashmail.com",
            "sharklasers.com",
            "dispostable.com"
    );

    public static boolean isDisposableEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase().trim();
        return DISPOSABLE_EMAIL_DOMAINS.contains(domain);
    }

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       EmailVerificationOtpRepository emailVerificationOtpRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailVerificationOtpRepository = emailVerificationOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.emailService = emailService;
    }

    /**
     * Strict Zero-Token Registration:
     * - Rejects disposable email domains.
     * - If an unverified account already exists, refreshes details and dispatches a fresh 6-digit Brevo OTP.
     * - For new accounts, creates user with is_verified = false and dispatches OTP.
     * - ALWAYS returns zero tokens ({ requiresVerification: true, accessToken: null, refreshToken: null }).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (isDisposableEmail(normalizedEmail)) {
            throw new BadRequestException("Disposable email addresses are not permitted. Please use a valid personal or academic email.");
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (Boolean.TRUE.equals(existing.getIsVerified())) {
                throw new BadRequestException("Email is already registered and verified: " + normalizedEmail);
            }

            // Update unverified user's credentials and details so user is never locked out
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
            existing.setFullName(request.getFullName().trim());
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                existing.setPhone(request.getPhone().trim());
            }
            existing.setIsActive(true);
            existing.setIsVerified(false);
            existing.setUpdatedAt(Instant.now());
            User savedUser = userRepository.save(existing);

            // Generate fresh 6-digit OTP
            int otpInt = secureRandom.nextInt(900_000) + 100_000;
            String otpCode = String.valueOf(otpInt);

            emailVerificationOtpRepository.deleteByEmailIgnoreCase(normalizedEmail);
            EmailVerificationOtp otpEntity = new EmailVerificationOtp();
            otpEntity.setUser(savedUser);
            otpEntity.setEmail(normalizedEmail);
            otpEntity.setOtpHash(passwordEncoder.encode(otpCode));
            otpEntity.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
            otpEntity.setAttemptCount(0);
            otpEntity.setLastSentAt(Instant.now());
            emailVerificationOtpRepository.save(otpEntity);

            emailService.sendOtpEmail(savedUser.getEmail(), savedUser.getFullName(), otpCode);

            List<String> roleNames = savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toList());
            return AuthResponse.verificationRequired(savedUser.getEmail(), savedUser.getFullName(), roleNames);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number is already in use: " + request.getPhone());
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());
        user.setIsActive(true);
        user.setIsVerified(false);

        // Assign default role
        String roleName = "ROLE_STUDENT";
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String requested = request.getRole().toUpperCase().trim();
            if (!requested.startsWith("ROLE_")) {
                requested = "ROLE_" + requested;
            }
            if (roleRepository.existsByName(requested)) {
                roleName = requested;
            }
        }

        final String targetRoleName = roleName;
        Role userRole = roleRepository.findByName(targetRoleName)
                .orElseGet(() -> roleRepository.save(new Role(targetRoleName, "Default role for " + targetRoleName)));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Generate cryptographically random 6-digit numeric OTP
        int otpInt = secureRandom.nextInt(900_000) + 100_000;
        String otpCode = String.valueOf(otpInt);

        // Save OTP record (valid 10 minutes)
        emailVerificationOtpRepository.deleteByEmailIgnoreCase(normalizedEmail);
        EmailVerificationOtp otpEntity = new EmailVerificationOtp();
        otpEntity.setUser(savedUser);
        otpEntity.setEmail(normalizedEmail);
        otpEntity.setOtpHash(passwordEncoder.encode(otpCode));
        otpEntity.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        otpEntity.setAttemptCount(0);
        otpEntity.setLastSentAt(Instant.now());
        emailVerificationOtpRepository.save(otpEntity);

        // Dispatch OTP via Brevo HTTPS API
        emailService.sendOtpEmail(savedUser.getEmail(), savedUser.getFullName(), otpCode);

        List<String> roleNames = savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toList());

        // Zero-token response forcing OTP verification
        return AuthResponse.verificationRequired(savedUser.getEmail(), savedUser.getFullName(), roleNames);
    }

    /**
     * Strict Login Gate:
     * - Authenticates email + password.
     * - Enforces is_verified == true check.
     * - If is_verified == false, refreshes Brevo OTP and returns HTTP 403 (EMAIL_NOT_VERIFIED) with NO tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User account not found"));

        // Strictly enforce verification check: NO unverified user receives a JWT token
        if (user.getIsVerified() == null || !Boolean.TRUE.equals(user.getIsVerified())) {
            // Pre-seeded campus accounts ending in @uplink.edu are auto-verified
            if (user.getEmail() != null && user.getEmail().toLowerCase().endsWith("@uplink.edu")) {
                user.setIsVerified(true);
                userRepository.save(user);
            } else {
                // Check 60-second cooldown before refreshing/sending OTP
                Optional<EmailVerificationOtp> latestOpt = emailVerificationOtpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail);
                boolean canSend = latestOpt.isEmpty()
                        || latestOpt.get().getLastSentAt() == null
                        || Duration.between(latestOpt.get().getLastSentAt(), Instant.now()).getSeconds() >= 60;

                if (canSend) {
                    int otpInt = secureRandom.nextInt(900_000) + 100_000;
                    String otpCode = String.valueOf(otpInt);

                    emailVerificationOtpRepository.deleteByEmailIgnoreCase(normalizedEmail);
                    EmailVerificationOtp otpEntity = new EmailVerificationOtp();
                    otpEntity.setUser(user);
                    otpEntity.setEmail(normalizedEmail);
                    otpEntity.setOtpHash(passwordEncoder.encode(otpCode));
                    otpEntity.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
                    otpEntity.setAttemptCount(0);
                    otpEntity.setLastSentAt(Instant.now());
                    emailVerificationOtpRepository.save(otpEntity);

                    emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otpCode);
                }

                throw new EmailNotVerifiedException(user.getEmail(), "Email verification required. A 6-digit code has been sent to your email.");
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String jwt = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId());

        List<String> roleNames = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new AuthResponse(jwt, refreshToken, userPrincipal.getId(), userPrincipal.getUsername(), userPrincipal.getFullName(), roleNames, true);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromJWT(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getIsVerified() == null || !Boolean.TRUE.equals(user.getIsVerified())) {
            throw new EmailNotVerifiedException(user.getEmail(), "Email verification required. A 6-digit code has been sent to your email.");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        String newJwt = tokenProvider.generateToken(auth);
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

        List<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());

        return new AuthResponse(newJwt, newRefreshToken, user.getId(), user.getEmail(), user.getFullName(), roleNames, user.getIsVerified());
    }

    /**
     * OTP Verification:
     * - Validates OTP hash, 10-minute expiry, and attempt_count < 5.
     * - On match: marks user verified, deletes OTPs, and NOW issues authenticated JWT + refresh token.
     */
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        String rawOtp = request.getOtp();

        if (rawOtp == null || rawOtp.isBlank()) {
            throw new BadRequestException("6-digit verification code is required");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));

        EmailVerificationOtp otpRecord = emailVerificationOtpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("No active verification code found for this email. Please request a new code."));

        if (otpRecord.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Verification code has expired. Please tap 'Resend Code' to receive a new one.");
        }

        if (otpRecord.getAttemptCount() >= 5) {
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new code.");
        }

        if (!passwordEncoder.matches(rawOtp.trim(), otpRecord.getOtpHash())) {
            otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);
            emailVerificationOtpRepository.save(otpRecord);
            int remaining = 5 - otpRecord.getAttemptCount();
            throw new BadRequestException(remaining > 0
                    ? "Invalid verification code. " + remaining + " attempt(s) remaining."
                    : "Maximum verification attempts exceeded. Please request a new code.");
        }

        // Mark user verified
        user.setIsVerified(true);
        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);

        // Delete used OTPs
        emailVerificationOtpRepository.deleteByEmailIgnoreCase(normalizedEmail);

        // Automatically authenticate user and issue JWT
        UserPrincipal userPrincipal = UserPrincipal.create(savedUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = tokenProvider.generateToken(auth);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getId());
        List<String> roleNames = savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toList());

        return new AuthResponse(jwt, refreshToken, savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), roleNames, true);
    }

    /**
     * Resend 6-Digit OTP:
     * - Rejects disposable emails.
     * - Enforces 60-second cooldown via last_sent_at.
     * - Generates fresh OTP and sends via Brevo.
     */
    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        if (isDisposableEmail(normalizedEmail)) {
            throw new BadRequestException("Disposable email addresses are not permitted.");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadRequestException("Email is already verified");
        }

        Optional<EmailVerificationOtp> latestOpt = emailVerificationOtpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail);
        if (latestOpt.isPresent() && latestOpt.get().getLastSentAt() != null) {
            long secondsElapsed = Duration.between(latestOpt.get().getLastSentAt(), Instant.now()).getSeconds();
            if (secondsElapsed < 60) {
                long waitSeconds = 60 - secondsElapsed;
                throw new BadRequestException("Please wait " + waitSeconds + " seconds before requesting a new code.");
            }
        }

        int otpInt = secureRandom.nextInt(900_000) + 100_000;
        String otpCode = String.valueOf(otpInt);

        emailVerificationOtpRepository.deleteByEmailIgnoreCase(normalizedEmail);
        EmailVerificationOtp otpEntity = new EmailVerificationOtp();
        otpEntity.setUser(user);
        otpEntity.setEmail(normalizedEmail);
        otpEntity.setOtpHash(passwordEncoder.encode(otpCode));
        otpEntity.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        otpEntity.setAttemptCount(0);
        otpEntity.setLastSentAt(Instant.now());
        emailVerificationOtpRepository.save(otpEntity);

        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otpCode);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String token = UUID.randomUUID().toString();
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
