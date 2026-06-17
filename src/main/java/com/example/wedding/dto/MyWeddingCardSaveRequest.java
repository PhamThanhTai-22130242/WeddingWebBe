package com.example.wedding.dto;

import java.util.List;

public record MyWeddingCardSaveRequest(
        String templateCode,
        String slug,
        String status,
        DesignRequest design,
        CoupleRequest couple,
        EventRequest event,
        List<MediaRequest> media
) {
    public record DesignRequest(
            String primaryColor,
            String dropEffect
    ) {
    }

    public record CoupleRequest(
            String groom,
            String bride,
            String groomRole,
            String brideRole,
            String groomFather,
            String groomMother,
            String brideFather,
            String brideMother
    ) {
    }

    public record EventRequest(
            String inviteText,
            String eventDate,
            String eventTime,
            String venueName,
            String address,
            String linkMap
    ) {
    }

    public record MediaRequest(
            String slotKey,
            String imgUrl,
            Integer number
    ) {
    }
}
