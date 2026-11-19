package com.example.Uplink_Backend.modules.qa.dto;

import java.util.List;

public class QuestionResponseDto {

    private String id;
    private String title;
    private String description;
    private List<String> topicTags;
    private String authorId;
    private String authorName;
    private String authorAcademicContext;
    private String authorAvatar;
    private String createdAt;
    private int answerCount;
    private int sameDoubtCount;
    private boolean hasUserMarkedSameDoubt;
    private boolean isUnanswered;

    public QuestionResponseDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<String> getTopicTags() {
        return topicTags;
    }

    public void setTopicTags(List<String> topicTags) {
        this.topicTags = topicTags;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAcademicContext() {
        return authorAcademicContext;
    }

    public void setAuthorAcademicContext(String authorAcademicContext) {
        this.authorAcademicContext = authorAcademicContext;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
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

    public boolean isHasUserMarkedSameDoubt() {
        return hasUserMarkedSameDoubt;
    }

    public void setHasUserMarkedSameDoubt(boolean hasUserMarkedSameDoubt) {
        this.hasUserMarkedSameDoubt = hasUserMarkedSameDoubt;
    }

    public boolean isUnanswered() {
        return isUnanswered;
    }

    public void setUnanswered(boolean unanswered) {
        isUnanswered = unanswered;
    }
}

