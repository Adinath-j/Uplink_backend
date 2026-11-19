package com.example.Uplink_Backend.modules.mentor.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateBookingRequest {

    @NotBlank(message = "Preferred time is required")
    private String preferredTime;

    @NotBlank(message = "Topic is required")
    private String topic;

    private String message;

    public CreateBookingRequest() {}

    public CreateBookingRequest(String preferredTime, String topic, String message) {
        this.preferredTime = preferredTime;
        this.topic = topic;
        this.message = message;
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
}

