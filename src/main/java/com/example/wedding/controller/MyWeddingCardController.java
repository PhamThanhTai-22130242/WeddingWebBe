package com.example.wedding.controller;

import com.example.wedding.config.APIResponse;
import com.example.wedding.config.ResponseStatus;
import com.example.wedding.dto.MyWeddingCardResponse;
import com.example.wedding.dto.MyWeddingCardSaveRequest;
import com.example.wedding.dto.SlugAvailabilityResponse;
import com.example.wedding.dto.WeddingCardWishManagementResponse;
import com.example.wedding.dto.WishVisibilityRequest;
import com.example.wedding.service.MyWeddingCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api/my-wedding-cards")
public class MyWeddingCardController {
    private final MyWeddingCardService myWeddingCardService;

    public MyWeddingCardController(MyWeddingCardService myWeddingCardService) {
        this.myWeddingCardService = myWeddingCardService;
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<MyWeddingCardResponse>>> getMine(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        List<MyWeddingCardResponse> response = myWeddingCardService.getMine(authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }

    @GetMapping("/slug-availability")
    public ResponseEntity<APIResponse<SlugAvailabilityResponse>> checkSlugAvailability(
            @RequestParam String slug,
            @RequestParam(name = "weddingId", required = false) Long weddingId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        SlugAvailabilityResponse response = myWeddingCardService.checkSlugAvailability(slug, weddingId, authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }

    @GetMapping("/{weddingId}")
    public ResponseEntity<APIResponse<MyWeddingCardResponse>> getById(
            @PathVariable Long weddingId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        MyWeddingCardResponse response = myWeddingCardService.getById(weddingId, authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }

    @GetMapping("/{weddingId}/wishes")
    public ResponseEntity<APIResponse<List<WeddingCardWishManagementResponse>>> getWishes(
            @PathVariable Long weddingId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        List<WeddingCardWishManagementResponse> response = myWeddingCardService.getWishes(weddingId, authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }

    @PatchMapping("/{weddingId}/wishes/{wishId}/visibility")
    public ResponseEntity<APIResponse<WeddingCardWishManagementResponse>> updateWishVisibility(
            @PathVariable Long weddingId,
            @PathVariable Long wishId,
            @RequestBody WishVisibilityRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        WeddingCardWishManagementResponse response = myWeddingCardService.updateWishVisibility(
                weddingId,
                wishId,
                request,
                authorizationHeader
        );
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }

    @PostMapping
    public ResponseEntity<APIResponse<MyWeddingCardResponse>> create(
            @RequestBody MyWeddingCardSaveRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        MyWeddingCardResponse response = myWeddingCardService.create(request, authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }

    @PutMapping("/{weddingId}")
    public ResponseEntity<APIResponse<MyWeddingCardResponse>> update(
            @PathVariable Long weddingId,
            @RequestBody MyWeddingCardSaveRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        MyWeddingCardResponse response = myWeddingCardService.update(weddingId, request, authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }
}
