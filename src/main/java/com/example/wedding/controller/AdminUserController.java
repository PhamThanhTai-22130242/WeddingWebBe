package com.example.wedding.controller;

import com.example.wedding.config.APIResponse;
import com.example.wedding.config.ResponseStatus;
import com.example.wedding.dto.AdminUserResponse;
import com.example.wedding.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<AdminUserResponse>>> getUsers(
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader
    ) {
        List<AdminUserResponse> users = userService.getUsersForAdmin(authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, users));
    }

    @PatchMapping({"/{userId}/lock", "/{userId}/block"})
    public ResponseEntity<APIResponse<AdminUserResponse>> lockUser(
            @PathVariable Long userId,
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader
    ) {
        AdminUserResponse user = userService.lockUserForAdmin(userId, authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS.getCode(), "Khóa tài khoản thành công", user));
    }

    @PatchMapping("/{userId}/unlock")
    public ResponseEntity<APIResponse<AdminUserResponse>> unlockUser(
            @PathVariable Long userId,
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader
    ) {
        AdminUserResponse user = userService.unlockUserForAdmin(userId, authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS.getCode(), "Mở khóa tài khoản thành công", user));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<APIResponse<AdminUserResponse>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader
    ) {
        AdminUserResponse user = userService.updateUserRoleForAdmin(userId, request == null ? null : request.get("role"), authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS.getCode(), "Cập nhật vai trò thành công", user));
    }
}
