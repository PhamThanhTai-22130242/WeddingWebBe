package com.example.wedding.service;

import com.example.wedding.dto.AdminUserResponse;
import com.example.wedding.dto.AuthResponse;
import com.example.wedding.dto.AuthResult;
import com.example.wedding.dto.AuthenticatedUser;
import com.example.wedding.dto.GoogleLoginRequest;
import com.example.wedding.dto.GoogleTokenInfo;
import com.example.wedding.dto.LoginRequest;
import com.example.wedding.dto.RegisterRequest;
import com.example.wedding.dto.UserResponse;
import com.example.wedding.exception.NotFoundException;
import com.example.wedding.exception.UnauthorizedException;
import com.example.wedding.repository.UserRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AccessTokenUserService accessTokenUserService;

    public UserService(UserRepository userRepository, JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, JwtService jwtService, GoogleTokenVerifier googleTokenVerifier, AccessTokenUserService accessTokenUserService) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.accessTokenUserService = accessTokenUserService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        String fullname = request.getFullname().trim();
        String role = "USER";
        String status = "ACTIVE";
        LocalDateTime now = LocalDateTime.now();
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into user (email, password, fullname, role, status, created_at, update_at) values (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, email);
            statement.setString(2, encodedPassword);
            statement.setString(3, fullname);
            statement.setString(4, role);
            statement.setString(5, status);
            statement.setObject(6, now);
            statement.setObject(7, now);
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        Long id = generatedId == null ? null : generatedId.longValue();
        return new UserResponse(id, email, fullname, role, status, now);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        AuthenticatedUser user = findByEmail(request.getEmail().trim().toLowerCase());
        ensureUserActive(user);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không đúng");
        }

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResult googleLogin(GoogleLoginRequest request) {
        GoogleTokenInfo googleUser = googleTokenVerifier.verify(request.getIdToken());
        AuthenticatedUser user = findByEmailOrCreateGoogleUser(googleUser);
        ensureUserActive(user);
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResult refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token không tồn tại");
        }

        JwtService.TokenPayload payload = jwtService.verifyRefreshToken(refreshToken);
        validateStoredRefreshToken(payload.getUserId(), refreshToken);
        revokeRefreshToken(refreshToken);

        AuthenticatedUser user = findById(payload.getUserId());
        ensureUserActive(user);
        return createAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        revokeRefreshToken(refreshToken);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsersForAdmin(String authorizationHeader) {
        verifyAdminAccess(authorizationHeader);
        return jdbcTemplate.query(
                "select id_user, email, fullname, role, status, created_at, update_at from user order by id_user desc",
                (rs, rowNum) -> mapAdminUser(rs)
        );
    }

    @Transactional
    public AdminUserResponse lockUserForAdmin(Long userId, String authorizationHeader) {
        AccessTokenUserService.CurrentUser admin = verifyAdminAccess(authorizationHeader);
        if (admin.id().equals(userId)) {
            throw new IllegalArgumentException("Admin không thể tự khóa tài khoản của mình");
        }

        int updatedRows = jdbcTemplate.update(
                "update user set status = ?, update_at = ? where id_user = ?",
                "BLOCKED",
                LocalDateTime.now(),
                userId
        );
        if (updatedRows == 0) {
            throw new NotFoundException("Tài khoản không tồn tại");
        }

        revokeRefreshTokensByUserId(userId);
        return findAdminUserById(userId);
    }

    @Transactional
    public AdminUserResponse unlockUserForAdmin(Long userId, String authorizationHeader) {
        verifyAdminAccess(authorizationHeader);

        int updatedRows = jdbcTemplate.update(
                "update user set status = ?, update_at = ? where id_user = ?",
                "ACTIVE",
                LocalDateTime.now(),
                userId
        );
        if (updatedRows == 0) {
            throw new NotFoundException("Tài khoản không tồn tại");
        }

        return findAdminUserById(userId);
    }

    private AccessTokenUserService.CurrentUser verifyAdminAccess(String authorizationHeader) {
        return accessTokenUserService.requireAdmin(authorizationHeader, "Không có quyền quản lý user");
    }

    private AdminUserResponse findAdminUserById(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select id_user, email, fullname, role, status, created_at, update_at from user where id_user = ? limit 1",
                    (rs, rowNum) -> mapAdminUser(rs),
                    userId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new NotFoundException("Tài khoản không tồn tại");
        }
    }

    private AdminUserResponse mapAdminUser(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AdminUserResponse(
                rs.getLong("id_user"),
                rs.getString("email"),
                rs.getString("fullname"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("update_at") == null ? null : rs.getTimestamp("update_at").toLocalDateTime()
        );
    }

    private void ensureUserActive(AuthenticatedUser user) {
        if ("BLOCKED".equalsIgnoreCase(user.getStatus()) || "LOCKED".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Tài khoản đã bị khóa");
        }
    }

    private AuthResult createAuthResponse(AuthenticatedUser user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = jwtService.createRefreshToken(user);
        saveRefreshToken(user.getId(), refreshToken);

        AuthResponse response = new AuthResponse(
                accessToken,
                jwtService.getAccessTokenExpirationSeconds(),
                user.toUserResponse()
        );
        return new AuthResult(response, refreshToken);
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        Instant expiresAt = Instant.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds());
        jdbcTemplate.update(
                "insert into user_refresh_tokens (user_id, refresh_token, expires_at, revoked, created_at) values (?, ?, ?, ?, ?)",
                userId,
                refreshToken,
                Timestamp.from(expiresAt),
                false,
                Timestamp.from(Instant.now())
        );
    }

    private void validateStoredRefreshToken(Long userId, String refreshToken) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_refresh_tokens where user_id = ? and refresh_token = ? and revoked = 0 and expires_at > ?",
                Integer.class,
                userId,
                refreshToken,
                Timestamp.from(Instant.now())
        );

        if (count == null || count == 0) {
            throw new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
    }

    private void revokeRefreshToken(String refreshToken) {
        jdbcTemplate.update(
                "update user_refresh_tokens set revoked = 1 where refresh_token = ?",
                refreshToken
        );
    }

    private void revokeRefreshTokensByUserId(Long userId) {
        jdbcTemplate.update(
                "update user_refresh_tokens set revoked = 1 where user_id = ?",
                userId
        );
    }

    private AuthenticatedUser findByEmailOrCreateGoogleUser(GoogleTokenInfo googleUser) {
        try {
            AuthenticatedUser user = findByEmail(googleUser.getEmail());
            jdbcTemplate.update(
                    "update user set provider = coalesce(provider, ?), google_sub = coalesce(google_sub, ?), picture = coalesce(picture, ?), update_at = ? where id_user = ?",
                    "GOOGLE",
                    googleUser.getSub(),
                    googleUser.getPicture(),
                    LocalDateTime.now(),
                    user.getId()
            );
            return user;
        } catch (UnauthorizedException exception) {
            return createGoogleUser(googleUser);
        }
    }

    private AuthenticatedUser createGoogleUser(GoogleTokenInfo googleUser) {
        String fullName = googleUser.getName().isBlank() ? googleUser.getEmail() : googleUser.getName();
        String encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        String role = "USER";
        String status = "ACTIVE";
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into user (email, password, fullname, role, status, created_at, update_at, provider, google_sub, picture) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, googleUser.getEmail());
            statement.setString(2, encodedPassword);
            statement.setString(3, fullName);
            statement.setString(4, role);
            statement.setString(5, status);
            statement.setObject(6, now);
            statement.setObject(7, now);
            statement.setString(8, "GOOGLE");
            statement.setString(9, googleUser.getSub());
            statement.setString(10, googleUser.getPicture());
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        Long id = generatedId == null ? null : generatedId.longValue();
        return new AuthenticatedUser(id, googleUser.getEmail(), encodedPassword, fullName, role, status, now);
    }

    private AuthenticatedUser findByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(
                    "select id_user, email, password, fullname, role, status, created_at from user where lower(email) = ? limit 1",
                    (rs, rowNum) -> new AuthenticatedUser(
                            rs.getLong("id_user"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("fullname"),
                            rs.getString("role"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                    ),
                    email
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new UnauthorizedException("Email hoặc mật khẩu không đúng");
        }
    }

    private AuthenticatedUser findById(Long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "select id_user, email, password, fullname, role, status, created_at from user where id_user = ? limit 1",
                    (rs, rowNum) -> new AuthenticatedUser(
                            rs.getLong("id_user"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("fullname"),
                            rs.getString("role"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                    ),
                    id
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new UnauthorizedException("Tài khoản không tồn tại");
        }
    }

    @Transactional
    public AdminUserResponse updateUserRoleForAdmin(Long userId, String role, String authorizationHeader) {
        AccessTokenUserService.CurrentUser admin = verifyAdminAccess(authorizationHeader);
        if (admin.id().equals(userId)) {
            throw new IllegalArgumentException("Admin không thể tự thay đổi vai trò của mình");
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Vai trò không hợp lệ");
        }

        String normalizedRole = role.trim().toUpperCase();
        if (!List.of("ADMIN", "USER", "SUPPORT").contains(normalizedRole)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ");
        }

        int updatedRows = jdbcTemplate.update(
                "update user set role = ?, update_at = ? where id_user = ?",
                normalizedRole,
                LocalDateTime.now(),
                userId
        );
        if (updatedRows == 0) {
            throw new NotFoundException("Tài khoản không tồn tại");
        }

        revokeRefreshTokensByUserId(userId);
        return findAdminUserById(userId);
    }
}
