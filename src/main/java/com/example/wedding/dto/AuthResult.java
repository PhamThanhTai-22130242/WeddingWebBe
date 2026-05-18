package com.example.wedding.dto;

public class AuthResult {
    private final AuthResponse response;
    private final String refreshToken;

    public AuthResult(AuthResponse response, String refreshToken) {
        this.response = response;
        this.refreshToken = refreshToken;
    }

    public AuthResponse getResponse() {
        return response;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
