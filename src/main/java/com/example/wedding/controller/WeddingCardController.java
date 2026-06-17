package com.example.wedding.controller;

import com.example.wedding.config.APIResponse;
import com.example.wedding.config.ResponseStatus;
import com.example.wedding.dto.WeddingCardDetailResponse;
import com.example.wedding.dto.WeddingCardWishManagementResponse;
import com.example.wedding.dto.WishRequest;
import com.example.wedding.dto.WishVisibilityRequest;
import com.example.wedding.dto.RsvpRequest;
import com.example.wedding.service.WeddingCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api/wedding-cards")
public class WeddingCardController {
    private final WeddingCardService weddingCardService;

    public WeddingCardController(WeddingCardService weddingCardService) {
        this.weddingCardService = weddingCardService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<APIResponse<WeddingCardDetailResponse>> getBySlug(@PathVariable String slug) {
        WeddingCardDetailResponse weddingCard = weddingCardService.getBySlug(slug);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, weddingCard));
    }

    @PostMapping("/{slug}/wishes")
    public ResponseEntity<APIResponse<WeddingCardDetailResponse.WishResponse>> createWish(
            @PathVariable String slug,
            @RequestBody WishRequest request
    ) {
        WeddingCardDetailResponse.WishResponse wish = weddingCardService.createWish(slug, request);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, wish));
    }

    @PostMapping("/{slug}/rsvp")
    public ResponseEntity<APIResponse<Void>> createRsvp(
            @PathVariable String slug,
            @RequestBody RsvpRequest request
    ) {
        weddingCardService.createRsvp(slug, request);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, null));
    }

    @GetMapping("/{slug}/wishes/manage")
    public ResponseEntity<APIResponse<List<WeddingCardWishManagementResponse>>> getWishesForManagement(
            @PathVariable String slug
    ) {
        List<WeddingCardWishManagementResponse> wishes = weddingCardService.getWishesForManagement(slug);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, wishes));
    }

    @PatchMapping("/{slug}/wishes/{wishId}/visibility")
    public ResponseEntity<APIResponse<WeddingCardWishManagementResponse>> updateWishVisibility(
            @PathVariable String slug,
            @PathVariable Long wishId,
            @RequestBody WishVisibilityRequest request
    ) {
        WeddingCardWishManagementResponse wish = weddingCardService.updateWishVisibility(slug, wishId, request);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, wish));
    }
}
