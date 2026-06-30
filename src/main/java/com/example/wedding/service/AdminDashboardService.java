package com.example.wedding.service;

import com.example.wedding.dto.AdminDashboardResponse;
import com.example.wedding.dto.AdminWeddingCardResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminDashboardService {
    private final JdbcTemplate jdbcTemplate;
    private final AccessTokenUserService accessTokenUserService;

    public AdminDashboardService(JdbcTemplate jdbcTemplate, AccessTokenUserService accessTokenUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.accessTokenUserService = accessTokenUserService;
    }

    private record CardRevenueInfo(LocalDateTime createdAt, String promoPrice) {}

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStats(String authorizationHeader) {
        accessTokenUserService.requireAdmin(authorizationHeader, "Không có quyền truy cập dashboard");

        // 1. Total Users
        Long totalUsersVal = jdbcTemplate.queryForObject("select count(*) from user", Long.class);
        long totalUsers = totalUsersVal == null ? 0 : totalUsersVal;

        // 2. Active Cards
        Long activeCardsVal = jdbcTemplate.queryForObject("select count(*) from wedding_card where lower(status) = 'active'", Long.class);
        long activeCards = activeCardsVal == null ? 0 : activeCardsVal;

        // 3. Total Cards
        Long totalCardsVal = jdbcTemplate.queryForObject("select count(*) from wedding_card", Long.class);
        long totalCards = totalCardsVal == null ? 0 : totalCardsVal;

        // 4. Total Revenue
        List<String> prices = jdbcTemplate.query(
                "select wt.promo_price from wedding_card wc join wedding_templates wt on wt.template_id = wc.template_id where lower(wc.status) = 'active'",
                (rs, rowNum) -> rs.getString("promo_price")
        );
        long sumRevenue = 0;
        for (String price : prices) {
            sumRevenue += parsePrice(price);
        }
        String totalRevenue = formatRevenue(sumRevenue);

        // 5. Revenue Chart (last 6 months in chronological order)
        List<YearMonth> yearMonths = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            yearMonths.add(currentMonth.minusMonths(i));
        }

        List<CardRevenueInfo> cardList = jdbcTemplate.query("""
                select coalesce(min(cp.created_at), u.created_at) as created_at, wt.promo_price
                from wedding_card wc
                join user u on u.id_user = wc.user_id
                join wedding_templates wt on wt.template_id = wc.template_id
                left join card_people cp on cp.wedding_id = wc.wedding_id
                where lower(wc.status) = 'active'
                group by wc.wedding_id, u.created_at, wt.promo_price
                """, (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime dt = ts == null ? null : ts.toLocalDateTime();
                    String priceStr = rs.getString("promo_price");
                    return new CardRevenueInfo(dt, priceStr);
                });

        Map<YearMonth, Long> monthlyRevenue = new HashMap<>();
        for (YearMonth ym : yearMonths) {
            monthlyRevenue.put(ym, 0L);
        }

        for (CardRevenueInfo card : cardList) {
            if (card.createdAt() != null) {
                YearMonth ym = YearMonth.from(card.createdAt());
                if (monthlyRevenue.containsKey(ym)) {
                    monthlyRevenue.put(ym, monthlyRevenue.get(ym) + parsePrice(card.promoPrice()));
                }
            }
        }

        List<AdminDashboardResponse.RevenueMonth> revenueChart = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        for (YearMonth ym : yearMonths) {
            long rev = monthlyRevenue.get(ym);
            revenueChart.add(new AdminDashboardResponse.RevenueMonth(ym.format(formatter), formatRevenueMillions(rev)));
        }

        // 6. Card Status Donut Stats
        List<Map<String, Object>> statusRows = jdbcTemplate.queryForList("select status, count(*) as count from wedding_card group by status");
        long activeCount = 0;
        long draftCount = 0;
        long lockedCount = 0;
        for (Map<String, Object> row : statusRows) {
            String statusVal = (String) row.get("status");
            long count = 0;
            Object countObj = row.get("count");
            if (countObj instanceof Number) {
                count = ((Number) countObj).longValue();
            }
            if (statusVal != null) {
                switch (statusVal.trim().toLowerCase(Locale.ROOT)) {
                    case "active" -> activeCount = count;
                    case "draft" -> draftCount = count;
                    case "locked" -> lockedCount = count;
                }
            }
        }
        AdminDashboardResponse.DonutStats cardStatus = new AdminDashboardResponse.DonutStats(activeCount, draftCount, lockedCount);

        // 7. New Users (last 5 registered users)
        List<AdminDashboardResponse.NewUserDto> newUsers = jdbcTemplate.query(
                "select fullname, email, created_at from user order by id_user desc limit 5",
                (rs, rowNum) -> {
                    String name = rs.getString("fullname");
                    String email = rs.getString("email");
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = ts == null ? null : ts.toLocalDateTime();
                    return new AdminDashboardResponse.NewUserDto(name, email, formatRelativeTime(createdAt));
                }
        );

        // 8. Recent Cards (last 5 active/created cards)
        List<AdminWeddingCardResponse> recentCards = jdbcTemplate.query("""
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
                where lower(wc.status) = 'active'
                group by wc.wedding_id, wc.slug, wc.status, u.id_user, u.fullname, u.email,
                         cve.number_views, u.created_at, cover.img_url, wt.preview_img,
                         wt.promo_price, wt.name, wt.category
                order by created_at desc, wc.wedding_id desc
                limit 5
                """, (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = ts == null ? null : ts.toLocalDateTime();
                    return new AdminWeddingCardResponse(
                            rs.getLong("wedding_id"),
                            rs.getString("groom_name"),
                            rs.getString("bride_name"),
                            rs.getString("slug"),
                            rs.getLong("creator_id"),
                            rs.getString("creator_name"),
                            rs.getString("creator_email"),
                            rs.getString("status"),
                            createdAt,
                            rs.getLong("view_count"),
                            rs.getString("preview_img"),
                            rs.getString("promo_price"),
                            rs.getString("template_name"),
                            rs.getInt("category")
                    );
                });

        return new AdminDashboardResponse(
                totalUsers,
                activeCards,
                totalCards,
                totalRevenue,
                revenueChart,
                cardStatus,
                newUsers,
                recentCards
        );
    }

    private long parsePrice(String price) {
        if (price == null) return 0;
        String clean = price.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) return 0;
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatRevenue(long amount) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(amount) + "đ";
    }

    private String formatRevenueMillions(long amount) {
        double millions = amount / 1000000.0;
        return String.format(Locale.US, "%.1fM", millions);
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Không rõ";
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return "Vừa xong";
        }
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " phút trước";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " giờ trước";
        }
        long days = duration.toDays();
        if (days < 30) {
            return days + " ngày trước";
        }
        long months = days / 30;
        if (months < 12) {
            return months + " tháng trước";
        }
        return (months / 12) + " năm trước";
    }
}
