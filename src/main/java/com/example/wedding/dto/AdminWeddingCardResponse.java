package com.example.wedding.dto;

import java.time.LocalDateTime;

public record AdminWeddingCardResponse(
        Long weddingId,
        String groomName,
        String brideName,
        String slug,
        Long creatorId,
        String creatorName,
        String creatorEmail,
        String status,
        LocalDateTime createdAt,
        Long viewCount,
        String previewImg,
        String promoPrice,
        String templateName,
        Integer category
) {
}
