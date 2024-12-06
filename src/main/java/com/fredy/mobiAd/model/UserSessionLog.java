package com.fredy.mobiAd.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class UserSessionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;
    private String phoneNumber;
    private String userInput;

    @Column(length = 5000)
    private String response;
    private LocalDateTime timestamp;

    // Default constructor
    public UserSessionLog() {}

    // Parameterized constructor
    public UserSessionLog(String sessionId, String phoneNumber, String userInput, String response) {
        this.sessionId = sessionId;
        this.phoneNumber = phoneNumber;
        this.userInput = userInput;
        this.response = response;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
