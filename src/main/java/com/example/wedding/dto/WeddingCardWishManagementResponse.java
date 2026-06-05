package com.example.wedding.dto;

public record WeddingCardWishManagementResponse(
        Long wishId,
        String guestName,
        String message,
        Boolean isApproved
) {
}
