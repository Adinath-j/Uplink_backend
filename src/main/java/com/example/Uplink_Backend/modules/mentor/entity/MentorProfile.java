package com.example.Uplink_Backend.modules.mentor.entity;

import com.example.Uplink_Backend.modules.auth.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_profiles")
public class MentorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 150)
    private String designation;

    @Column(nullable = false)
    private String organization;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String domains; // Comma-separated: "Engineering, Product, Data Science"

    @Column(columnDefinition = "TEXT")
    private String skills; // Comma-separated: "System Design, Microservices"

    @Column(name = "years_of_experience")
    private int yearsOfExperience = 0;

    @Column(name = "is_available")
    private boolean isAvailable = true;

    @Column(name = "available_days", length = 100)
    private String availableDays = "Mon, Wed, Fri";

    private double rating = 5.0;

    @Column(name = "sessions_completed")
    private int sessionsCompleted = 0;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public MentorProfile() {}

    public MentorProfile(String name, String designation, String organization, String avatarUrl,
                         String bio, String domains, String skills, int yearsOfExperience, String availableDays) {
        this.name = name;
        this.designation = designation;
        this.organization = organization;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.domains = domains;
        this.skills = skills;
        this.yearsOfExperience = yearsOfExperience;
        this.availableDays = availableDays != null ? availableDays : "Mon, Wed, Fri";
        this.isAvailable = true;
        this.rating = 5.0;
        this.sessionsCompleted = 0;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDomains() {
        return domains;
    }

    public void setDomains(String domains) {
        this.domains = domains;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getSessionsCompleted() {
        return sessionsCompleted;
    }

    public void setSessionsCompleted(int sessionsCompleted) {
        this.sessionsCompleted = sessionsCompleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

