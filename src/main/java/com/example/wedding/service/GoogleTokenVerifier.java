package com.example.wedding.service;

import com.example.wedding.dto.GoogleTokenInfo;
import com.example.wedding.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleTokenVerifier {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String googleClientId;

    public GoogleTokenVerifier(
            ObjectMapper objectMapper,
            @Value("${app.google.client-id}") String googleClientId
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.googleClientId = googleClientId;
    }

    public GoogleTokenInfo verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Google idToken khong hop le");
        }

        JsonNode tokenInfo = fetchTokenInfo(idToken);
        String audience = getRequiredText(tokenInfo, "aud");
        if (!googleClientId.equals(audience)) {
            throw new UnauthorizedException("Google token sai client id");
        }

        String emailVerified = tokenInfo.path("email_verified").asText("false");
        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new UnauthorizedException("Email Google chua duoc xac minh");
        }

        return new GoogleTokenInfo(
                getRequiredText(tokenInfo, "sub"),
                getRequiredText(tokenInfo, "email").trim().toLowerCase(),
                tokenInfo.path("name").asText(""),
                tokenInfo.path("picture").asText("")
        );
    }

    private JsonNode fetchTokenInfo(String idToken) {
        String encodedToken = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedToken))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new UnauthorizedException("Google idToken khong hop le");
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new UnauthorizedException("Khong the verify Google idToken");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UnauthorizedException("Khong the verify Google idToken");
        }
    }

    private String getRequiredText(JsonNode jsonNode, String fieldName) {
        String value = jsonNode.path(fieldName).asText("");
        if (value.isBlank()) {
            throw new UnauthorizedException("Google idToken thieu thong tin " + fieldName);
        }
        return value;
    }
}
