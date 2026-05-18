package com.example.wedding.dto;

import java.time.LocalDateTime;

public class AdminUserResponse {
    private final Long id;
    private final String email;
    private final String fullname;
    private final String role;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updateAt;

    public AdminUserResponse(Long id, String email, String fullname, String role, String status, LocalDateTime createdAt, LocalDateTime updateAt) {
        this.id = id;
        this.email = email;
        this.fullname = fullname;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updateAt = updateAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
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

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }
}
