package com.example.wedding.service;

import com.example.wedding.exception.ForbiddenException;
import com.example.wedding.exception.UnauthorizedException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenUserService {
    private final JdbcTemplate jdbcTemplate;
    private final JwtService jwtService;

    public AccessTokenUserService(JdbcTemplate jdbcTemplate, JwtService jwtService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtService = jwtService;
    }

    public CurrentUser requireActiveUser(String authorizationHeader) {
        JwtService.TokenPayload payload = verifyAccessToken(authorizationHeader);
        CurrentUser user = findCurrentUser(payload.getUserId());
        if ("BLOCKED".equalsIgnoreCase(user.status()) || "LOCKED".equalsIgnoreCase(user.status())) {
            throw new UnauthorizedException("Tài khoản đã bị khóa");
        }
        return user;
    }

    public CurrentUser requireAdmin(String authorizationHeader, String forbiddenMessage) {
        CurrentUser user = requireActiveUser(authorizationHeader);
        if (!"ADMIN".equalsIgnoreCase(user.role())) {
            throw new ForbiddenException(forbiddenMessage);
        }
        return user;
    }

    private JwtService.TokenPayload verifyAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Thiếu access token");
        }

        String accessToken = authorizationHeader.substring("Bearer ".length()).trim();
        if (accessToken.isBlank()) {
            throw new UnauthorizedException("Thiếu access token");
        }

        return jwtService.verifyAccessToken(accessToken);
    }

    private CurrentUser findCurrentUser(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select id_user, role, status from user where id_user = ? limit 1",
                    (rs, rowNum) -> new CurrentUser(
                            rs.getLong("id_user"),
                            rs.getString("role"),
                            rs.getString("status")
                    ),
                    userId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new UnauthorizedException("Tài khoản không tồn tại");
        }
    }

    public record CurrentUser(Long id, String role, String status) {
    }
}
