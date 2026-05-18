package com.example.wedding.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record WeddingCardDetailResponse(
        String slug,
        String themeColor,
        String dropEffect,
        TemplateResponse template,
        MusicTrackResponse musicTrack,
        ViewEventResponse viewEvent,
        List<PersonResponse> people,
        List<EventResponse> events,
        List<MediaResponse> media,
        List<GiftAccountResponse> giftAccounts,
        List<WishResponse> wishes
) {
    public record TemplateResponse(
            String name,
            String code,
            String previewImg
    ) {
    }

    public record MusicTrackResponse(
            String fileUrl,
            Integer timeStart
    ) {
    }

    public record ViewEventResponse(
            Long numberViews
    ) {
    }

    public record PersonResponse(
            String role,
            String fullName,
            String shortName,
            String avatar,
            String fatherName,
            String motherName,
            String familyLable
    ) {
    }

    public record EventResponse(
            String inviteText,
            LocalDate eventDate,
            LocalTime eventTime,
            String venueName,
            String address,
            String linkMap
    ) {
    }

    public record MediaResponse(
            String imgUrl,
            Integer number
    ) {
    }

    public record GiftAccountResponse(
            String targetPerson,
            String bankName,
            String bankCode,
            String qrImg,
            String accountName,
            String accountNumber
    ) {
    }

    public record WishResponse(
            String guestName,
            String message,
            Boolean isApproved
    ) {
    }
}
