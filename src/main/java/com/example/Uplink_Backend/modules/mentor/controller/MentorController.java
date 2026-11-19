package com.example.Uplink_Backend.modules.mentor.controller;

import com.example.Uplink_Backend.common.dto.ApiResponse;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.modules.mentor.dto.*;
import com.example.Uplink_Backend.modules.mentor.service.MentorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mentors")
@Tag(name = "14. Mentorship", description = "Mentor Directory, 1-on-1 Guidance, and Session Booking")
public class MentorController {

    private final MentorService mentorService;

    public MentorController(MentorService mentorService) {
        this.mentorService = mentorService;
    }

    @GetMapping
    @Operation(summary = "Browse mentor directory with domain filters and keyword search")
    public ResponseEntity<ApiResponse<Page<MentorProfileDto>>> getMentors(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<MentorProfileDto> mentors = mentorService.getMentors(domain, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Mentors retrieved", mentors));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed mentor profile by ID")
    public ResponseEntity<ApiResponse<MentorProfileDto>> getMentorById(@PathVariable UUID id) {
        MentorProfileDto mentor = mentorService.getMentorById(id);
        return ResponseEntity.ok(ApiResponse.ok("Mentor details retrieved", mentor));
    }

    @PostMapping
    @Operation(summary = "Register or create a mentor profile")
    public ResponseEntity<ApiResponse<MentorProfileDto>> createMentorProfile(
            @Valid @RequestBody CreateMentorRequest request) {
        MentorProfileDto created = mentorService.createMentorProfile(request);
        return ResponseEntity.ok(ApiResponse.created("Mentor profile created", created));
    }

    @PostMapping("/{id}/bookings")
    @Operation(summary = "Request a mentorship session booking")
    public ResponseEntity<ApiResponse<BookingResponseDto>> requestBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponseDto booking = mentorService.requestBooking(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.created("Booking request submitted", booking));
    }

    @GetMapping("/bookings/my")
    @Operation(summary = "Get all mentorship booking requests for active student")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<BookingResponseDto> bookings = mentorService.getMyBookings(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok("My booking requests retrieved", bookings));
    }

    @PatchMapping("/bookings/{bookingId}/status")
    @Operation(summary = "Update booking status (CONFIRMED, DECLINED, COMPLETED)")
    public ResponseEntity<ApiResponse<BookingResponseDto>> updateBookingStatus(
            @PathVariable UUID bookingId,
            @RequestParam String status) {
        BookingResponseDto updated = mentorService.updateBookingStatus(bookingId, status);
        return ResponseEntity.ok(ApiResponse.ok("Booking status updated", updated));
    }
}

