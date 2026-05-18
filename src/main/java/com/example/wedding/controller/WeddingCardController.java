package com.example.wedding.controller;

import com.example.wedding.config.APIResponse;
import com.example.wedding.config.ResponseStatus;
import com.example.wedding.dto.WeddingCardDetailResponse;
import com.example.wedding.service.WeddingCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
