package com.example.wedding.dto;

import java.time.LocalDateTime;

public class AuthenticatedUser {
    private final Long id;
    private final String email;
    private final String password;
    private final String fullname;
    private final String role;
    private final String status;
    private final LocalDateTime createdAt;

    public AuthenticatedUser(Long id, String email, String password, String fullname, String role, String status, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public String getFullname() {
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

    public UserResponse toUserResponse() {
        return new UserResponse(id, email, fullname, role, status, createdAt);
    }
}
