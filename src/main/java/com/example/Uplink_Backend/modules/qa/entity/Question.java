package com.example.Uplink_Backend.modules.qa.entity;

import com.example.Uplink_Backend.modules.auth.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "topic_tags", columnDefinition = "TEXT")
    private String topicTags; // Comma-separated tags (e.g. "DSA, Web Development")

    @Column(name = "answer_count")
    private int answerCount = 0;

    @Column(name = "same_doubt_count")
    private int sameDoubtCount = 0;

    @Column(name = "is_unanswered")
    private boolean isUnanswered = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Question() {}

    public Question(User author, String title, String description, String topicTags) {
        this.author = author;
        this.title = title;
        this.description = description;
        this.topicTags = topicTags;
        this.answerCount = 0;
        this.sameDoubtCount = 0;
        this.isUnanswered = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTopicTags() {
        return topicTags;
    }

    public void setTopicTags(String topicTags) {
        this.topicTags = topicTags;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public void setAnswerCount(int answerCount) {
        this.answerCount = answerCount;
    }

    public int getSameDoubtCount() {
        return sameDoubtCount;
    }

    public void setSameDoubtCount(int sameDoubtCount) {
        this.sameDoubtCount = sameDoubtCount;
    }

    public boolean isUnanswered() {
        return isUnanswered;
    }

    public void setUnanswered(boolean unanswered) {
        isUnanswered = unanswered;
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

