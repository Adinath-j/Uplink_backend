package com.example.Uplink_Backend.modules.qa.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateQuestionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private List<String> topicTags;

    public CreateQuestionRequest() {}

    public CreateQuestionRequest(String title, String description, List<String> topicTags) {
        this.title = title;
        this.description = description;
        this.topicTags = topicTags;
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
}

