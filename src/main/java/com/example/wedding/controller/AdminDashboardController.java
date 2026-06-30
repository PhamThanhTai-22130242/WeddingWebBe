package com.example.wedding.controller;

import com.example.wedding.config.APIResponse;
import com.example.wedding.config.ResponseStatus;
import com.example.wedding.dto.AdminDashboardResponse;
import com.example.wedding.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public ResponseEntity<APIResponse<AdminDashboardResponse>> getDashboardStats(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        AdminDashboardResponse response = adminDashboardService.getDashboardStats(authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, response));
    }
}
