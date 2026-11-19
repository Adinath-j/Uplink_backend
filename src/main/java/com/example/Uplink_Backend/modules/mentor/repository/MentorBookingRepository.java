package com.example.Uplink_Backend.modules.mentor.repository;

import com.example.Uplink_Backend.modules.mentor.entity.MentorBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MentorBookingRepository extends JpaRepository<MentorBooking, UUID> {
    List<MentorBooking> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<MentorBooking> findByMentorIdOrderByCreatedAtDesc(UUID mentorId);
}

