package com.example.wedding.dto;

import java.util.List;

public record AdminWeddingCardPageResponse(
        List<AdminWeddingCardResponse> items,
        long totalItems,
        int page,
        int size,
        int totalPages
) {
}
