package com.example.Uplink_Backend.modules.mentor.service;

import com.example.Uplink_Backend.common.exception.ResourceNotFoundException;
import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.repository.UserRepository;
import com.example.Uplink_Backend.modules.mentor.dto.*;
import com.example.Uplink_Backend.modules.mentor.entity.*;
import com.example.Uplink_Backend.modules.mentor.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MentorService {

    private final MentorProfileRepository mentorRepository;
    private final MentorBookingRepository bookingRepository;
    private final UserRepository userRepository;

    public MentorService(MentorProfileRepository mentorRepository,
                         MentorBookingRepository bookingRepository,
                         UserRepository userRepository) {
        this.mentorRepository = mentorRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public Page<MentorProfileDto> getMentors(String domain, String search, Pageable pageable) {
        String cleanDomain = (domain != null && !domain.equalsIgnoreCase("All") && !domain.isBlank()) ? domain.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<MentorProfile> page = mentorRepository.searchMentors(cleanDomain, cleanSearch, pageable);

        List<MentorProfileDto> dtos = page.getContent().stream()
                .map(this::mapToMentorDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public MentorProfileDto getMentorById(UUID id) {
        MentorProfile mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MentorProfile", "id", id));
        return mapToMentorDto(mentor);
    }

    @Transactional
    public MentorProfileDto createMentorProfile(CreateMentorRequest request) {
        String domainsStr = request.getDomains() != null ? String.join(", ", request.getDomains()) : "";
        String skillsStr = request.getSkills() != null ? String.join(", ", request.getSkills()) : "";

        MentorProfile mentor = new MentorProfile(
                request.getName().trim(),
                request.getDesignation().trim(),
                request.getOrganization().trim(),
                request.getAvatarUrl(),
                request.getBio(),
                domainsStr,
                skillsStr,
                request.getYearsOfExperience(),
                request.getAvailableDays()
        );

        MentorProfile saved = mentorRepository.save(mentor);
        return mapToMentorDto(saved);
    }

    @Transactional
    public BookingResponseDto requestBooking(UUID studentId, UUID mentorId, CreateBookingRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        MentorProfile mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("MentorProfile", "id", mentorId));

        MentorBooking booking = new MentorBooking(
                mentor,
                student,
                mentor.getName(),
                mentor.getDesignation() + ", " + mentor.getOrganization(),
                request.getPreferredTime().trim(),
                request.getTopic().trim(),
                request.getMessage() != null ? request.getMessage().trim() : ""
        );

        MentorBooking saved = bookingRepository.save(booking);
        return mapToBookingDto(saved);
    }

    public List<BookingResponseDto> getMyBookings(UUID studentId) {
        return bookingRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::mapToBookingDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDto updateBookingStatus(UUID bookingId, String status) {
        MentorBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("MentorBooking", "id", bookingId));

        booking.setStatus(status.toUpperCase().trim());
        booking.setUpdatedAt(Instant.now());
        MentorBooking saved = bookingRepository.save(booking);
        return mapToBookingDto(saved);
    }

    private MentorProfileDto mapToMentorDto(MentorProfile m) {
        MentorProfileDto dto = new MentorProfileDto();
        dto.setId(m.getId().toString());
        dto.setName(m.getName());
        dto.setDesignation(m.getDesignation());
        dto.setOrganization(m.getOrganization());
        dto.setAvatar(m.getAvatarUrl());
        dto.setBio(m.getBio());

        List<String> domainList = new ArrayList<>();
        if (m.getDomains() != null && !m.getDomains().isBlank()) {
            domainList = Arrays.stream(m.getDomains().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        dto.setDomains(domainList);

        List<String> skillList = new ArrayList<>();
        if (m.getSkills() != null && !m.getSkills().isBlank()) {
            skillList = Arrays.stream(m.getSkills().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        dto.setSkills(skillList);

        dto.setYearsOfExperience(m.getYearsOfExperience());
        dto.setAvailable(m.isAvailable());
        dto.setAvailableDays(m.getAvailableDays());
        dto.setRating(m.getRating());
        dto.setSessionsCompleted(m.getSessionsCompleted());
        return dto;
    }

    private BookingResponseDto mapToBookingDto(MentorBooking b) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(b.getId().toString());
        dto.setMentorId(b.getMentor().getId().toString());
        dto.setMentorName(b.getMentorName());
        dto.setMentorRole(b.getMentorRole());
        dto.setPreferredTime(b.getPreferredTime());
        dto.setTopic(b.getTopic());
        dto.setMessage(b.getMessage());
        dto.setStatus(b.getStatus());
        dto.setCreatedAt(b.getCreatedAt().toString());
        return dto;
    }
}

