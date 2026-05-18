package com.example.wedding.dto;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String email;
    private String fullname;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public UserResponse(Long id, String email, String fullname, String role, String status, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullname = fullname;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullname;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
