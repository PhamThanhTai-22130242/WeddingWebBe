package com.example.wedding.dto;

public class AuthResponse {
    private final String accessToken;
    private final long expiresIn;
    private final UserResponse user;

    public AuthResponse(String accessToken, long expiresIn, UserResponse user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public UserResponse getUser() {
        return user;
    }
}
