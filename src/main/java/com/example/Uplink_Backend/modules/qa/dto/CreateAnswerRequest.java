package com.example.Uplink_Backend.modules.qa.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAnswerRequest {

    @NotBlank(message = "Answer content is required")
    private String content;

    public CreateAnswerRequest() {}

    public CreateAnswerRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

