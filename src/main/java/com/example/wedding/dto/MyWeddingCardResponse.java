package com.example.wedding.dto;

import java.util.List;

public record MyWeddingCardResponse(
        Long weddingId,
        String slug,
        String status,
        Long viewCount,
        String themeColor,
        String dropEffect,
        WeddingCardDetailResponse.TemplateResponse template,
        List<WeddingCardDetailResponse.PersonResponse> people,
        List<WeddingCardDetailResponse.EventResponse> events,
        List<WeddingCardDetailResponse.MediaResponse> media
) {
}
