package com.example.wedding.controller;

import com.example.wedding.config.APIResponse;
import com.example.wedding.config.ResponseStatus;
import com.example.wedding.dto.AuthResponse;
import com.example.wedding.dto.AuthResult;
import com.example.wedding.dto.GoogleLoginRequest;
import com.example.wedding.dto.LoginRequest;
import com.example.wedding.dto.RegisterRequest;
import com.example.wedding.dto.UserResponse;
import com.example.wedding.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final long refreshTokenExpirationSeconds;
    private final boolean refreshCookieSecure;

    public AuthController(
            UserService userService,
            @Value("${app.jwt.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds,
            @Value("${app.auth.refresh-cookie-secure:false}") boolean refreshCookieSecure
    ) {
        this.userService = userService;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<APIResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse<>(ResponseStatus.CREATED, user));
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResult authResult = userService.login(request);
        return ResponseEntity.ok()
                .header("Set-Cookie", createRefreshTokenCookie(authResult.getRefreshToken()).toString())
                .body(new APIResponse<>(ResponseStatus.SUCCESS, authResult.getResponse()));
    }

    @PostMapping("/google")
    public ResponseEntity<APIResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResult authResult = userService.googleLogin(request);
        return ResponseEntity.ok()
                .header("Set-Cookie", createRefreshTokenCookie(authResult.getRefreshToken()).toString())
                .body(new APIResponse<>(200, "Đăng nhập Google thành công", authResult.getResponse()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<APIResponse<AuthResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        AuthResult authResult = userService.refreshToken(refreshToken);
        return ResponseEntity.ok()
                .header("Set-Cookie", createRefreshTokenCookie(authResult.getRefreshToken()).toString())
                .body(new APIResponse<>(ResponseStatus.SUCCESS, authResult.getResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<APIResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        userService.logout(refreshToken);
        return ResponseEntity.ok()
                .header("Set-Cookie", clearRefreshTokenCookie().toString())
                .body(new APIResponse<>(ResponseStatus.SUCCESS.getCode(), "Đăng xuất thành công", null));
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(refreshTokenExpirationSeconds)
                .build();
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }
}
