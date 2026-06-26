package com.example.wedding.dto;

import java.util.List;

public record AdminDashboardResponse(
    long totalUsers,
    long activeCards,
    long totalCards,
    String totalRevenue,
    List<RevenueMonth> revenueChart,
    DonutStats cardStatus,
    List<NewUserDto> newUsers,
    List<AdminWeddingCardResponse> recentCards
) {
    public record RevenueMonth(String month, String value) {}
    public record DonutStats(long active, long draft, long locked) {}
    public record NewUserDto(String name, String email, String time) {}
}
