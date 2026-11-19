package com.example.Uplink_Backend.modules.mentor.entity;

import com.example.Uplink_Backend.modules.auth.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_bookings")
public class MentorBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mentor_id", nullable = false)
    private MentorProfile mentor;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "mentor_name", nullable = false)
    private String mentorName;

    @Column(name = "mentor_role")
    private String mentorRole;

    @Column(name = "preferred_time", nullable = false, length = 100)
    private String preferredTime;

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String status = "REQUESTED"; // REQUESTED, CONFIRMED, DECLINED, COMPLETED

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public MentorBooking() {}

    public MentorBooking(MentorProfile mentor, User student, String mentorName, String mentorRole,
                         String preferredTime, String topic, String message) {
        this.mentor = mentor;
        this.student = student;
        this.mentorName = mentorName;
        this.mentorRole = mentorRole;
        this.preferredTime = preferredTime;
        this.topic = topic;
        this.message = message;
        this.status = "REQUESTED";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MentorProfile getMentor() {
        return mentor;
    }

    public void setMentor(MentorProfile mentor) {
        this.mentor = mentor;
    }

    @JsonIgnore
    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public String getMentorName() {
        return mentorName;
    }

    public void setMentorName(String mentorName) {
        this.mentorName = mentorName;
    }

    public String getMentorRole() {
        return mentorRole;
    }

    public void setMentorRole(String mentorRole) {
        this.mentorRole = mentorRole;
    }

    public String getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTime = preferredTime;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

