package com.example.wedding.service;

import com.example.wedding.dto.WeddingCardDetailResponse;
import com.example.wedding.dto.WeddingCardWishManagementResponse;
import com.example.wedding.dto.WishRequest;
import com.example.wedding.dto.WishVisibilityRequest;
import com.example.wedding.dto.RsvpRequest;
import com.example.wedding.exception.NotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class WeddingCardService {
    private final JdbcTemplate jdbcTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public WeddingCardService(JdbcTemplate jdbcTemplate, SimpMessagingTemplate messagingTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public WeddingCardDetailResponse getBySlug(String slug) {
        WeddingCardBase base = findBaseBySlug(slug);

        return new WeddingCardDetailResponse(
                base.slug(),
                base.themeColor(),
                base.dropEffect(),
                base.template(),
                base.musicTrack(),
                base.viewEvent(),
                jdbcTemplate.query("""
                        select card_id, role, full_name, short_name, avatar, father_name, mother_name,
                               family_lable
                        from card_people
                        where wedding_id = ?
                        order by card_id asc
                        """, (rs, rowNum) -> mapPerson(rs), base.id()),
                jdbcTemplate.query("""
                        select wedding_event_id, invite_text, event_date, event_time, venue_name, address, link_map
                        from wedding_events
                        where wedding_id = ?
                        order by wedding_event_id asc
                        """, (rs, rowNum) -> mapEvent(rs), base.id()),
                jdbcTemplate.query("""
                        select cm.card_media_id, tms.slot_key, cm.img_url, cm.number
                        from card_media cm
                        left join tempate_media_slots tms on tms.template_media_id = cm.template_media_id
                        where cm.wedding_id = ?
                        order by cm.number asc, cm.card_media_id asc
                        """, (rs, rowNum) -> mapMedia(rs), base.id()),
                jdbcTemplate.query("""
                        select gift_account_id, target_person, bank_name, bank_code, qr_img,
                               account_name, account_number
                        from gift_accounts
                        where wedding_id = ?
                        order by gift_account_id asc
                        """, (rs, rowNum) -> mapGiftAccount(rs), base.id()),
                jdbcTemplate.query("""
                        select wish_id, guest_name, message, is_approved
                        from wishes
                        where wedding_id = ?
                        order by `created-at` desc
                        """, (rs, rowNum) -> mapWish(rs), base.id())
        );
    }

    @Transactional
    public WeddingCardDetailResponse.WishResponse createWish(String slug, WishRequest request) {
        WeddingCardBase base = findBaseBySlug(slug);
        String guestName = request == null ? "" : trim(request.name());
        String message = request == null ? "" : trim(request.message());

        if (guestName.isEmpty() || message.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên và lời chúc");
        }
        if (guestName.length() > 30) {
            throw new IllegalArgumentException("Tên của bạn không được quá 30 ký tự");
        }
        if (message.length() > 300) {
            throw new IllegalArgumentException("Lời chúc không được quá 300 ký tự");
        }

        org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement("""
                    insert into wishes (wedding_id, guest_name, message, is_approved)
                    values (?, ?, ?, 1)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, base.id());
            ps.setString(2, guestName);
            ps.setString(3, message);
            return ps;
        }, keyHolder);
        
        Long wishId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;

        WeddingCardDetailResponse.WishResponse response = new WeddingCardDetailResponse.WishResponse(
                wishId,
                guestName,
                message,
                true
        );

        messagingTemplate.convertAndSend("/topic/wedding-cards/" + base.slug() + "/wishes", response);
        return response;
    }

    @Transactional
    public void createRsvp(String slug, RsvpRequest request) {
        WeddingCardBase base = findBaseBySlug(slug);
        String guestName = request == null ? "" : trim(request.name());
        String status = request == null ? "yes" : trim(request.attending());

        if (guestName.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên");
        }
        if (guestName.length() > 255) {
            throw new IllegalArgumentException("Tên không được quá 255 ký tự");
        }

        jdbcTemplate.update("""
                insert into rsvp_responses (wedding_id, fullname, status, created_at)
                values (?, ?, ?, now())
                """, base.id(), guestName, status);
    }

    @Transactional(readOnly = true)
    public List<WeddingCardWishManagementResponse> getWishesForManagement(String slug) {
        WeddingCardBase base = findBaseBySlug(slug);

        return jdbcTemplate.query("""
                select wish_id, guest_name, message, is_approved
                from wishes
                where wedding_id = ?
                order by wish_id desc
                """, (rs, rowNum) -> mapWishManagement(rs), base.id());
    }

    @Transactional
    public WeddingCardWishManagementResponse updateWishVisibility(String slug, Long wishId, WishVisibilityRequest request) {
        WeddingCardBase base = findBaseBySlug(slug);
        boolean approved = request != null && Boolean.TRUE.equals(request.approved());

        int updated = jdbcTemplate.update("""
                update wishes
                set is_approved = ?
                where wish_id = ? and wedding_id = ?
                """, approved, wishId, base.id());
        if (updated == 0) {
            throw new NotFoundException("Không tìm thấy lời chúc");
        }

        return jdbcTemplate.queryForObject("""
                select wish_id, guest_name, message, is_approved
                from wishes
                where wish_id = ? and wedding_id = ?
                """, (rs, rowNum) -> mapWishManagement(rs), wishId, base.id());
    }

    private WeddingCardBase findBaseBySlug(String slug) {
        try {
            return jdbcTemplate.queryForObject("""
                    select wc.wedding_id, wc.slug, wc.theme_color, wc.drop_effect,
                           wt.template_id, wt.name as template_name, wt.code as template_code,
                           wt.preview_img,
                           mt.music_track_id, mt.file_url, mt.time_start,
                           cve.view_id, cve.number_views
                    from wedding_card wc
                    join wedding_templates wt on wt.template_id = wc.template_id
                    left join music_track mt on mt.music_track_id = wc.music_track_id
                    left join card_view_events cve on cve.wedding_id = wc.wedding_id
                    where wc.slug = ? and lower(wc.status) = 'active'
                    limit 1
                    """, (rs, rowNum) -> mapBase(rs), slug);
        } catch (EmptyResultDataAccessException exception) {
            throw new NotFoundException("Wedding card not found or not active with slug: " + slug);
        }
    }

    private WeddingCardBase mapBase(ResultSet rs) throws SQLException {
        WeddingCardDetailResponse.TemplateResponse template = new WeddingCardDetailResponse.TemplateResponse(
                rs.getLong("template_id"),
                rs.getString("template_name"),
                rs.getString("template_code"),
                rs.getString("preview_img")
        );

        Long musicTrackId = getLong(rs, "music_track_id");
        WeddingCardDetailResponse.MusicTrackResponse musicTrack = musicTrackId == null ? null : new WeddingCardDetailResponse.MusicTrackResponse(
                rs.getString("file_url"),
                getInteger(rs, "time_start")
        );

        Long viewId = getLong(rs, "view_id");
        WeddingCardDetailResponse.ViewEventResponse viewEvent = viewId == null ? null : new WeddingCardDetailResponse.ViewEventResponse(
                getLong(rs, "number_views")
        );

        return new WeddingCardBase(
                rs.getLong("wedding_id"),
                rs.getString("slug"),
                rs.getString("theme_color"),
                rs.getString("drop_effect"),
                template,
                musicTrack,
                viewEvent
        );
    }

    private WeddingCardDetailResponse.PersonResponse mapPerson(ResultSet rs) throws SQLException {
        return new WeddingCardDetailResponse.PersonResponse(
                rs.getString("role"),
                rs.getString("full_name"),
                rs.getString("short_name"),
                rs.getString("avatar"),
                rs.getString("father_name"),
                rs.getString("mother_name"),
                rs.getString("family_lable")
        );
    }

    private WeddingCardDetailResponse.EventResponse mapEvent(ResultSet rs) throws SQLException {
        return new WeddingCardDetailResponse.EventResponse(
                rs.getString("invite_text"),
                getLocalDate(rs, "event_date"),
                getLocalTime(rs, "event_time"),
                rs.getString("venue_name"),
                rs.getString("address"),
                rs.getString("link_map")
        );
    }

    private WeddingCardDetailResponse.MediaResponse mapMedia(ResultSet rs) throws SQLException {
        return new WeddingCardDetailResponse.MediaResponse(
                rs.getString("slot_key"),
                rs.getString("img_url"),
                getInteger(rs, "number")
        );
    }

    private WeddingCardDetailResponse.GiftAccountResponse mapGiftAccount(ResultSet rs) throws SQLException {
        return new WeddingCardDetailResponse.GiftAccountResponse(
                rs.getString("target_person"),
                rs.getString("bank_name"),
                rs.getString("bank_code"),
                rs.getString("qr_img"),
                rs.getString("account_name"),
                rs.getString("account_number")
        );
    }

    private WeddingCardDetailResponse.WishResponse mapWish(ResultSet rs) throws SQLException {
        return new WeddingCardDetailResponse.WishResponse(
                rs.getLong("wish_id"),
                rs.getString("guest_name"),
                rs.getString("message"),
                getBoolean(rs, "is_approved")
        );
    }

    private WeddingCardWishManagementResponse mapWishManagement(ResultSet rs) throws SQLException {
        return new WeddingCardWishManagementResponse(
                rs.getLong("wish_id"),
                rs.getString("guest_name"),
                rs.getString("message"),
                getBoolean(rs, "is_approved")
        );
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalTime getLocalTime(ResultSet rs, String column) throws SQLException {
        java.sql.Time time = rs.getTime(column);
        return time == null ? null : time.toLocalTime();
    }

    private record WeddingCardBase(
            Long id,
            String slug,
            String themeColor,
            String dropEffect,
            WeddingCardDetailResponse.TemplateResponse template,
            WeddingCardDetailResponse.MusicTrackResponse musicTrack,
            WeddingCardDetailResponse.ViewEventResponse viewEvent
    ) {
    }
}
