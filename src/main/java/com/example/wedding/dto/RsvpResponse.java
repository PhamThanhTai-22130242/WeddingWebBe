package com.example.wedding.dto;

import java.time.LocalDateTime;

public record RsvpResponse(
        Long id,
        String fullname,
        String status,
        LocalDateTime createdAt
) {
}
