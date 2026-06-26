package com.example.wedding.controller;

import com.example.wedding.config.APIResponse;
import com.example.wedding.config.ResponseStatus;
import com.example.wedding.dto.AdminWeddingCardPageResponse;
import com.example.wedding.dto.AdminWeddingCardResponse;
import com.example.wedding.service.AdminWeddingCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api/admin/invitations")
public class AdminWeddingCardController {
    private final AdminWeddingCardService adminWeddingCardService;

    public AdminWeddingCardController(AdminWeddingCardService adminWeddingCardService) {
        this.adminWeddingCardService = adminWeddingCardService;
    }

    @GetMapping
    public ResponseEntity<APIResponse<AdminWeddingCardPageResponse>> getWeddingCards(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String creatorRole,
            @RequestParam(defaultValue = "today") String dateFilter,
            @RequestParam(defaultValue = "created_desc") String sort
    ) {
        AdminWeddingCardPageResponse response = adminWeddingCardService.getWeddingCards(
                authorizationHeader,
                page,
                size,
                query,
                status,
                userId,
                creatorRole,
                dateFilter,
                sort
        );
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }

    @GetMapping("/templates/stats")
    public ResponseEntity<APIResponse<Map<String, Long>>> getTemplateStats(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        Map<String, Long> stats = adminWeddingCardService.getTemplateStats(authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, stats));
    }

    @PatchMapping("/{weddingId}/status")
    public ResponseEntity<APIResponse<AdminWeddingCardResponse>> updateWeddingCardStatus(
            @PathVariable Long weddingId,
            @RequestBody Map<String, String> request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        AdminWeddingCardResponse response = adminWeddingCardService.updateWeddingCardStatus(
                authorizationHeader,
                weddingId,
                request == null ? null : request.get("status")
        );
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS.getCode(), "Cập nhật trạng thái thiệp thành công", response));
    }
}
