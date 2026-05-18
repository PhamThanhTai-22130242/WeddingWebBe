package com.example.wedding.service;

import com.example.wedding.dto.AuthenticatedUser;
import com.example.wedding.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final String secret;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret:wedding-secret-key-change-me}") String secret,
            @Value("${app.jwt.access-token-expiration-seconds:900}") long accessTokenExpirationSeconds,
            @Value("${app.jwt.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds
    ) {
        this.secret = secret;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String createAccessToken(AuthenticatedUser user) {
        return createToken(user, "access", accessTokenExpirationSeconds);
    }

    public String createRefreshToken(AuthenticatedUser user) {
        return createToken(user, "refresh", refreshTokenExpirationSeconds);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    public TokenPayload verifyRefreshToken(String token) {
        TokenPayload payload = verifyToken(token);
        if (!"refresh".equals(payload.getType())) {
            throw new UnauthorizedException("Refresh token không hợp lệ");
        }
        return payload;
    }

    public TokenPayload verifyAccessToken(String token) {
        TokenPayload payload = verifyToken(token);
        if (!"access".equals(payload.getType())) {
            throw new UnauthorizedException("Access token không hợp lệ");
        }
        return payload;
    }

    private String createToken(AuthenticatedUser user, String type, long expirationSeconds) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{"
                + "\"sub\":\"" + user.getId() + "\","
                + "\"email\":\"" + escapeJson(user.getEmail()) + "\","
                + "\"role\":\"" + escapeJson(user.getRole()) + "\","
                + "\"type\":\"" + type + "\","
                + "\"iat\":" + issuedAt + ","
                + "\"exp\":" + expiresAt
                + "}";

        String encodedHeader = encode(header);
        String encodedPayload = encode(payload);
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    private TokenPayload verifyToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedException("Token không hợp lệ");
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new UnauthorizedException("Token không hợp lệ");
        }

        String payloadJson = new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
        long expiresAt = extractLong(payloadJson, "exp");
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new UnauthorizedException("Token đã hết hạn");
        }

        return new TokenPayload(
                Long.valueOf(extractString(payloadJson, "sub")),
                extractString(payloadJson, "email"),
                extractString(payloadJson, "role"),
                extractString(payloadJson, "type")
        );
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo token", exception);
        }
    }

    private String encode(String value) {
        return BASE64_URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }

    private String extractString(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new UnauthorizedException("Token không hợp lệ");
        }
        return matcher.group(1);
    }

    private long extractLong(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new UnauthorizedException("Token không hợp lệ");
        }
        return Long.parseLong(matcher.group(1));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static class TokenPayload {
        private final Long userId;
        private final String email;
        private final String role;
        private final String type;

        public TokenPayload(Long userId, String email, String role, String type) {
            this.userId = userId;
            this.email = email;
            this.role = role;
            this.type = type;
        }
        public Long getUserId() {
            return userId;
        }
        public String getEmail() {
            return email;
        }
        public String getRole() {
            return role;
        }
        public String getType() {
            return type;
        }
    }
}
