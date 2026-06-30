package com.example.wedding.service;

import com.example.wedding.dto.AdminWeddingCardPageResponse;
import com.example.wedding.dto.AdminWeddingCardResponse;
import com.example.wedding.exception.NotFoundException;
import com.example.wedding.exception.ForbiddenException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

@Service
public class AdminWeddingCardService {
    private final JdbcTemplate jdbcTemplate;
    private final AccessTokenUserService accessTokenUserService;

    public AdminWeddingCardService(JdbcTemplate jdbcTemplate, AccessTokenUserService accessTokenUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.accessTokenUserService = accessTokenUserService;
    }

    @Transactional(readOnly = true)
    public AdminWeddingCardPageResponse getWeddingCards(
            String authorizationHeader,
            int page,
            int size,
            String query,
            String status,
            Long userId,
            String creatorRole,
            String dateFilter,
            String sort
    ) {
        verifyAdminAccess(authorizationHeader);

        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        int offset = (safePage - 1) * safeSize;
        List<Object> params = new ArrayList<>();
        String whereClause = buildWhereClause(query, status, userId, creatorRole, dateFilter, params);
        String orderBy = switch (sort == null ? "" : sort) {
            case "views_desc" -> "coalesce(cve.number_views, 0) desc, wc.wedding_id desc";
            case "created_asc" -> "created_at asc, wc.wedding_id asc";
            default -> "created_at desc, wc.wedding_id desc";
        };

        Long total = jdbcTemplate.queryForObject("""
                select count(*)
                from wedding_card wc
                join user u on u.id_user = wc.user_id
                %s
                """.formatted(whereClause), Long.class, params.toArray());

        List<Object> listParams = new ArrayList<>(params);
        listParams.add(safeSize);
        listParams.add(offset);
        List<AdminWeddingCardResponse> items = jdbcTemplate.query("""
                select wc.wedding_id, wc.slug, wc.status,
                       u.id_user as creator_id, u.fullname as creator_name, u.email as creator_email,
                       coalesce(cve.number_views, 0) as view_count,
                       coalesce(min(cp.created_at), u.created_at) as created_at,
                       max(case when cp.role = 'groom' then coalesce(cp.short_name, cp.full_name) end) as groom_name,
                       max(case when cp.role = 'bride' then coalesce(cp.short_name, cp.full_name) end) as bride_name,
                       coalesce(cover.img_url, wt.preview_img) as preview_img,
                       wt.promo_price as promo_price, wt.name as template_name, wt.category as category
                from wedding_card wc
                join user u on u.id_user = wc.user_id
                join wedding_templates wt on wt.template_id = wc.template_id
                left join card_view_events cve on cve.wedding_id = wc.wedding_id
                left join card_people cp on cp.wedding_id = wc.wedding_id
                left join (
                    select cm.wedding_id, min(cm.img_url) as img_url
                    from card_media cm
                    join tempate_media_slots tms on tms.template_media_id = cm.template_media_id
                    where tms.slot_key = 'images.cover'
                    group by cm.wedding_id
                ) cover on cover.wedding_id = wc.wedding_id
                %s
                group by wc.wedding_id, wc.slug, wc.status, u.id_user, u.fullname, u.email,
                         cve.number_views, u.created_at, cover.img_url, wt.preview_img,
                         wt.promo_price, wt.name, wt.category
                order by %s
                limit ? offset ?
                """.formatted(whereClause, orderBy), (rs, rowNum) -> mapWeddingCard(rs), listParams.toArray());

        long totalItems = total == null ? 0 : total;
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) safeSize);
        return new AdminWeddingCardPageResponse(items, totalItems, safePage, safeSize, totalPages);
    }

    @Transactional
    public AdminWeddingCardResponse updateWeddingCardStatus(
            String authorizationHeader,
            Long weddingId,
            String status
    ) {
        verifyAdminAccess(authorizationHeader);

        if (weddingId == null) {
            throw new IllegalArgumentException("Thiếu mã thiệp cưới");
        }

        String normalizedStatus = normalizeStatus(status);
        int updatedRows = jdbcTemplate.update(
                "update wedding_card set status = ? where wedding_id = ?",
                normalizedStatus,
                weddingId
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Thiệp cưới không tồn tại");
        }

        return findWeddingCardById(weddingId);
    }

    private AdminWeddingCardResponse findWeddingCardById(Long weddingId) {
        List<AdminWeddingCardResponse> cards = jdbcTemplate.query("""
                select wc.wedding_id, wc.slug, wc.status,
                       u.id_user as creator_id, u.fullname as creator_name, u.email as creator_email,
                       coalesce(cve.number_views, 0) as view_count,
                       coalesce(min(cp.created_at), u.created_at) as created_at,
                       max(case when cp.role = 'groom' then coalesce(cp.short_name, cp.full_name) end) as groom_name,
                       max(case when cp.role = 'bride' then coalesce(cp.short_name, cp.full_name) end) as bride_name,
                       coalesce(cover.img_url, wt.preview_img) as preview_img,
                       wt.promo_price as promo_price, wt.name as template_name, wt.category as category
                from wedding_card wc
                join user u on u.id_user = wc.user_id
                join wedding_templates wt on wt.template_id = wc.template_id
                left join card_view_events cve on cve.wedding_id = wc.wedding_id
                left join card_people cp on cp.wedding_id = wc.wedding_id
                left join (
                    select cm.wedding_id, min(cm.img_url) as img_url
                    from card_media cm
                    join tempate_media_slots tms on tms.template_media_id = cm.template_media_id
                    where tms.slot_key = 'images.cover'
                    group by cm.wedding_id
                ) cover on cover.wedding_id = wc.wedding_id
                where wc.wedding_id = ?
                group by wc.wedding_id, wc.slug, wc.status, u.id_user, u.fullname, u.email,
                         cve.number_views, u.created_at, cover.img_url, wt.preview_img,
                         wt.promo_price, wt.name, wt.category
                """, (rs, rowNum) -> mapWeddingCard(rs), weddingId);

        if (cards.isEmpty()) {
            throw new NotFoundException("Thiệp cưới không tồn tại");
        }

        return cards.get(0);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái thiệp không hợp lệ");
        }

        String normalizedStatus = status.trim().toLowerCase(Locale.ROOT);
        if (!List.of("active", "draft", "locked").contains(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái thiệp không hợp lệ");
        }

        return normalizedStatus;
    }

    private String buildWhereClause(String query, String status, Long userId, String creatorRole, String dateFilter, List<Object> params) {
        List<String> conditions = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String keyword = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            conditions.add("""
                    (
                        lower(wc.slug) like ?
                        or lower(u.fullname) like ?
                        or lower(u.email) like ?
                        or exists (
                            select 1
                            from card_people cp_search
                            where cp_search.wedding_id = wc.wedding_id
                              and lower(coalesce(cp_search.short_name, cp_search.full_name, '')) like ?
                        )
                    )
                    """);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            conditions.add("lower(wc.status) = ?");
            params.add(status.trim().toLowerCase(Locale.ROOT));
        }

        if (userId != null) {
            conditions.add("wc.user_id = ?");
            params.add(userId);
        }

        if (creatorRole != null && !creatorRole.isBlank()) {
            if ("ADMIN".equalsIgnoreCase(creatorRole)) {
                conditions.add("lower(u.role) = 'admin'");
            } else if ("USER".equalsIgnoreCase(creatorRole)) {
                conditions.add("coalesce(lower(u.role), '') != 'admin'");
            }
        }

        if (dateFilter != null && !dateFilter.isBlank() && !"all".equalsIgnoreCase(dateFilter)) {
            if ("today".equalsIgnoreCase(dateFilter)) {
                conditions.add("date(coalesce((select min(created_at) from card_people cp where cp.wedding_id = wc.wedding_id), u.created_at)) = current_date()");
            } else {
                try {
                    int days = Integer.parseInt(dateFilter.trim());
                    conditions.add("coalesce((select min(created_at) from card_people cp where cp.wedding_id = wc.wedding_id), u.created_at) >= date_sub(now(), interval ? day)");
                    params.add(days);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        if (conditions.isEmpty()) {
            return "";
        }
        return "where " + String.join(" and ", conditions);
    }

    private AdminWeddingCardResponse mapWeddingCard(ResultSet rs) throws SQLException {
        return new AdminWeddingCardResponse(
                rs.getLong("wedding_id"),
                rs.getString("groom_name"),
                rs.getString("bride_name"),
                rs.getString("slug"),
                rs.getLong("creator_id"),
                rs.getString("creator_name"),
                rs.getString("creator_email"),
                rs.getString("status"),
                getLocalDateTime(rs, "created_at"),
                rs.getLong("view_count"),
                rs.getString("preview_img"),
                rs.getString("promo_price"),
                rs.getString("template_name"),
                rs.getInt("category")
        );
    }

    private AccessTokenUserService.CurrentUser verifyAdminAccess(String authorizationHeader) {
        AccessTokenUserService.CurrentUser user = accessTokenUserService.requireActiveUser(authorizationHeader);
        if (!"ADMIN".equalsIgnoreCase(user.role()) && !"SUPPORT".equalsIgnoreCase(user.role())) {
            throw new ForbiddenException("Không có quyền quản lý thiệp cưới");
        }
        return user;
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getTemplateStats(String authorizationHeader) {
        verifyAdminAccess(authorizationHeader);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select wt.code as template_code, count(wc.wedding_id) as active_count
                from wedding_card wc
                join wedding_templates wt on wt.template_id = wc.template_id
                where lower(wc.status) = 'active'
                group by wt.code
                """);

        Map<String, Long> stats = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String code = (String) row.get("template_code");
            Long count = 0L;
            Object val = row.get("active_count");
            if (val instanceof Number) {
                count = ((Number) val).longValue();
            }
            stats.put(code, count);
        }
        return stats;
    }
}
