package com.example.wedding.service;

import com.example.wedding.dto.MyWeddingCardResponse;
import com.example.wedding.dto.MyWeddingCardSaveRequest;
import com.example.wedding.dto.SlugAvailabilityResponse;
import com.example.wedding.dto.WeddingCardDetailResponse;
import com.example.wedding.dto.WeddingCardWishManagementResponse;
import com.example.wedding.dto.WishVisibilityRequest;
import com.example.wedding.dto.RsvpResponse;
import com.example.wedding.exception.ForbiddenException;
import com.example.wedding.exception.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class MyWeddingCardService {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final JdbcTemplate jdbcTemplate;
    private final AccessTokenUserService accessTokenUserService;

    public MyWeddingCardService(JdbcTemplate jdbcTemplate, AccessTokenUserService accessTokenUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.accessTokenUserService = accessTokenUserService;
    }

    @Transactional(readOnly = true)
    public MyWeddingCardResponse getById(Long weddingId, String authorizationHeader) {
        Long userId = getCurrentUserId(authorizationHeader);
        ensureOwnedByUser(weddingId, userId);
        return getResponse(weddingId);
    }

    @Transactional(readOnly = true)
    public List<MyWeddingCardResponse> getMine(String authorizationHeader) {
        Long userId = getCurrentUserId(authorizationHeader);
        List<Long> weddingIds = jdbcTemplate.query("""
                select wedding_id
                from wedding_card
                where user_id = ?
                order by wedding_id desc
                """, (rs, rowNum) -> rs.getLong("wedding_id"), userId);

        return weddingIds.stream()
                .map(this::getResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SlugAvailabilityResponse checkSlugAvailability(String slug, Long weddingId, String authorizationHeader) {
        Long userId = getCurrentUserId(authorizationHeader);
        if (weddingId != null) {
            ensureOwnedByUser(weddingId, userId);
        }

        String requestedSlug = slugify(slug);
        if (!isBlank(requestedSlug) && slugExists(requestedSlug, weddingId)) {
            throw new IllegalArgumentException("URL đã tồn tại. Bạn vui lòng nhập URL khác");
        }

        return new SlugAvailabilityResponse(true);
    }

    @Transactional(readOnly = true)
    public List<WeddingCardWishManagementResponse> getWishes(Long weddingId, String authorizationHeader) {
        Long userId = getCurrentUserId(authorizationHeader);
        ensureOwnedByUser(weddingId, userId);

        return jdbcTemplate.query("""
                select wish_id, guest_name, message, is_approved
                from wishes
                where wedding_id = ?
                order by wish_id desc
                """, (rs, rowNum) -> mapWishManagementResponse(rs), weddingId);
    }

    @Transactional(readOnly = true)
    public List<RsvpResponse> getRsvps(Long weddingId, String authorizationHeader) {
        Long userId = getCurrentUserId(authorizationHeader);
        ensureOwnedByUser(weddingId, userId);

        return jdbcTemplate.query("""
                select rsvp_responses_id, fullname, status, created_at
                from rsvp_responses
                where wedding_id = ?
                order by rsvp_responses_id desc
                """, (rs, rowNum) -> new RsvpResponse(
                        rs.getLong("rsvp_responses_id"),
                        rs.getString("fullname"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
                ), weddingId);
    }

    @Transactional
    public WeddingCardWishManagementResponse updateWishVisibility(
            Long weddingId,
            Long wishId,
            WishVisibilityRequest request,
            String authorizationHeader
    ) {
        Long userId = getCurrentUserId(authorizationHeader);
        ensureOwnedByUser(weddingId, userId);
        boolean approved = request != null && Boolean.TRUE.equals(request.approved());

        int updated = jdbcTemplate.update("""
                update wishes
                set is_approved = ?
                where wish_id = ? and wedding_id = ?
                """, approved, wishId, weddingId);
        if (updated == 0) {
            throw new NotFoundException("Không tìm thấy lời chúc");
        }

        return getWishManagementResponse(weddingId, wishId);
    }

    @Transactional
    public MyWeddingCardResponse create(MyWeddingCardSaveRequest request, String authorizationHeader) {
        Long userId = getCurrentUserId(authorizationHeader);
        TemplateRecord template = findTemplate(request.templateCode());

        String slug = createUniqueSlug(request, null);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into wedding_card (template_id, user_id, slug, status, theme_color, drop_effect)
                    values (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, template.id());
            statement.setLong(2, userId);
            statement.setString(3, slug);
            statement.setString(4, normalizeStatus(request.status()));
            statement.setString(5, request.design() == null ? null : request.design().primaryColor());
            statement.setString(6, request.design() == null ? null : request.design().dropEffect());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Không thể tạo thiệp cưới");
        }

        Long weddingId = key.longValue();
        replaceChildren(weddingId, request);
        return getResponse(weddingId);
    }

    @Transactional
    public MyWeddingCardResponse update(Long weddingId, MyWeddingCardSaveRequest request, String authorizationHeader) {
        Long userId = getCurrentUserId(authorizationHeader);
        ensureOwnedByUser(weddingId, userId);
        TemplateRecord template = findTemplate(request.templateCode());

        String slug = createUniqueSlug(request, weddingId);
        jdbcTemplate.update("""
                update wedding_card
                set template_id = ?, slug = ?, status = ?, theme_color = ?, drop_effect = ?
                where wedding_id = ? and user_id = ?
                """,
                template.id(),
                slug,
                normalizeStatus(request.status()),
                request.design() == null ? null : request.design().primaryColor(),
                request.design() == null ? null : request.design().dropEffect(),
                weddingId,
                userId);

        replaceChildren(weddingId, request);
        return getResponse(weddingId);
    }

    private void replaceChildren(Long weddingId, MyWeddingCardSaveRequest request) {
        jdbcTemplate.update("delete from card_media where wedding_id = ?", weddingId);
        jdbcTemplate.update("delete from wedding_events where wedding_id = ?", weddingId);
        jdbcTemplate.update("delete from card_people where wedding_id = ?", weddingId);

        MyWeddingCardSaveRequest.CoupleRequest couple = request.couple();
        if (couple != null) {
            insertPerson(weddingId, "groom", couple.groom(), couple.groom(), couple.groomRole(), couple.groomFather(), couple.groomMother());
            insertPerson(weddingId, "bride", couple.bride(), couple.bride(), couple.brideRole(), couple.brideFather(), couple.brideMother());
        }

        MyWeddingCardSaveRequest.EventRequest event = request.event();
        if (event != null) {
            jdbcTemplate.update("""
                    insert into wedding_events (wedding_id, invite_text, event_date, event_time, venue_name, address, link_map)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    weddingId,
                    event.inviteText(),
                    parseDate(event.eventDate()),
                    parseTime(event.eventTime()),
                    event.venueName(),
                    event.address(),
                    event.linkMap());
        }

        List<MyWeddingCardSaveRequest.MediaRequest> media = request.media();
        if (media != null) {
            int fallbackNumber = 1;
            for (MyWeddingCardSaveRequest.MediaRequest item : media) {
                if (isBlank(item.slotKey()) || isBlank(item.imgUrl())) {
                    continue;
                }
                int number = item.number() == null ? fallbackNumber : item.number();
                Long slotId = ensureMediaSlot(item.slotKey(), number);
                jdbcTemplate.update("""
                        insert into card_media (wedding_id, img_url, template_media_id, number)
                        values (?, ?, ?, ?)
                        """, weddingId, item.imgUrl(), slotId, number);
                fallbackNumber++;
            }
        }
    }

    private void insertPerson(Long weddingId, String role, String fullName, String shortName, String familyLabel, String fatherName, String motherName) {
        jdbcTemplate.update("""
                insert into card_people (wedding_id, role, full_name, short_name, family_lable, father_name, mother_name)
                values (?, ?, ?, ?, ?, ?, ?)
                """, weddingId, role, fullName, shortName, familyLabel, fatherName, motherName);
    }

    private Long ensureMediaSlot(String slotKey, int number) {
        List<Long> ids = jdbcTemplate.query("""
                select template_media_id
                from tempate_media_slots
                where slot_key = ?
                order by template_media_id asc
                limit 1
                """, (rs, rowNum) -> rs.getLong("template_media_id"), slotKey);

        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into tempate_media_slots (slot_key, slot_lable, media_type, max_item)
                    values (?, ?, 'image', 1)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, slotKey);
            statement.setString(2, "Emerald image " + number);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Không thể tạo vị trí ảnh");
        }
        return key.longValue();
    }

    private MyWeddingCardResponse getResponse(Long weddingId) {
        return jdbcTemplate.queryForObject("""
                select wc.wedding_id, wc.slug, wc.status, wc.theme_color, wc.drop_effect,
                       wt.template_id, wt.name as template_name, wt.code as template_code, wt.preview_img,
                       cve.number_views
                from wedding_card wc
                join wedding_templates wt on wt.template_id = wc.template_id
                left join card_view_events cve on cve.wedding_id = wc.wedding_id
                where wc.wedding_id = ?
                """, (rs, rowNum) -> mapResponse(rs), weddingId);
    }

    private WeddingCardWishManagementResponse getWishManagementResponse(Long weddingId, Long wishId) {
        return jdbcTemplate.queryForObject("""
                select wish_id, guest_name, message, is_approved
                from wishes
                where wedding_id = ? and wish_id = ?
                """, (rs, rowNum) -> mapWishManagementResponse(rs), weddingId, wishId);
    }

    private WeddingCardWishManagementResponse mapWishManagementResponse(ResultSet rs) throws SQLException {
        return new WeddingCardWishManagementResponse(
                rs.getLong("wish_id"),
                rs.getString("guest_name"),
                rs.getString("message"),
                getBoolean(rs, "is_approved")
        );
    }

    private MyWeddingCardResponse mapResponse(ResultSet rs) throws SQLException {
        Long weddingId = rs.getLong("wedding_id");
        WeddingCardDetailResponse.TemplateResponse template = new WeddingCardDetailResponse.TemplateResponse(
                rs.getLong("template_id"),
                rs.getString("template_name"),
                rs.getString("template_code"),
                rs.getString("preview_img")
        );

        return new MyWeddingCardResponse(
                weddingId,
                rs.getString("slug"),
                rs.getString("status"),
                getLong(rs, "number_views"),
                rs.getString("theme_color"),
                rs.getString("drop_effect"),
                template,
                jdbcTemplate.query("""
                        select role, full_name, short_name, avatar, father_name, mother_name, family_lable
                        from card_people
                        where wedding_id = ?
                        order by card_id asc
                        """, (personRs, rowNum) -> new WeddingCardDetailResponse.PersonResponse(
                        personRs.getString("role"),
                        personRs.getString("full_name"),
                        personRs.getString("short_name"),
                        personRs.getString("avatar"),
                        personRs.getString("father_name"),
                        personRs.getString("mother_name"),
                        personRs.getString("family_lable")
                ), weddingId),
                jdbcTemplate.query("""
                        select invite_text, event_date, event_time, venue_name, address, link_map
                        from wedding_events
                        where wedding_id = ?
                        order by wedding_event_id asc
                        """, (eventRs, rowNum) -> new WeddingCardDetailResponse.EventResponse(
                        eventRs.getString("invite_text"),
                        getLocalDate(eventRs, "event_date"),
                        getLocalTime(eventRs, "event_time"),
                        eventRs.getString("venue_name"),
                        eventRs.getString("address"),
                        eventRs.getString("link_map")
                ), weddingId),
                jdbcTemplate.query("""
                        select tms.slot_key, cm.img_url, cm.number
                        from card_media cm
                        left join tempate_media_slots tms on tms.template_media_id = cm.template_media_id
                        where cm.wedding_id = ?
                        order by cm.number asc, cm.card_media_id asc
                        """, (mediaRs, rowNum) -> new WeddingCardDetailResponse.MediaResponse(
                        mediaRs.getString("slot_key"),
                        mediaRs.getString("img_url"),
                        getInteger(mediaRs, "number")
                ), weddingId)
        );
    }

    private TemplateRecord findTemplate(String code) {
        if (isBlank(code)) {
            throw new IllegalArgumentException("Thiếu mã mẫu thiệp");
        }

        List<TemplateRecord> templates = jdbcTemplate.query("""
                select template_id, code
                from wedding_templates
                where code = ? and coalesce(is_active, 1) = 1
                limit 1
                """, (rs, rowNum) -> new TemplateRecord(rs.getLong("template_id"), rs.getString("code")), code);

        if (templates.isEmpty()) {
            throw new NotFoundException("Không tìm thấy mẫu thiệp: " + code);
        }
        return templates.get(0);
    }

    private void ensureOwnedByUser(Long weddingId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from wedding_card where wedding_id = ? and user_id = ?",
                Integer.class,
                weddingId,
                userId);
        if (count == null || count == 0) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa thiệp này");
        }
    }

    private Long getCurrentUserId(String authorizationHeader) {
        return accessTokenUserService.requireActiveUser(authorizationHeader).id();
    }

    private String createUniqueSlug(MyWeddingCardSaveRequest request, Long currentWeddingId) {
        String requestedSlug = slugify(request.slug());
        if (!isBlank(requestedSlug)) {
            if (slugExists(requestedSlug, currentWeddingId)) {
                throw new IllegalArgumentException("URL đã tồn tại. Bạn vui lòng nhập URL khác");
            }
            return requestedSlug;
        }

        String baseSlug = slugify(getName(request.couple() == null ? null : request.couple().groom())
                + " " + getName(request.couple() == null ? null : request.couple().bride()));
        if (isBlank(baseSlug)) {
            baseSlug = slugify(request.templateCode());
        }
        if (isBlank(baseSlug)) {
            baseSlug = "wedding-invitation";
        }

        return nextAvailableSlug(baseSlug, currentWeddingId);
    }

    private String nextAvailableSlug(String baseSlug, Long currentWeddingId) {
        String slug = baseSlug;
        int suffix = 2;
        while (slugExists(slug, currentWeddingId)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }
        return slug;
    }

    private boolean slugExists(String slug, Long currentWeddingId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from wedding_card
                where slug = ? and (? is null or wedding_id <> ?)
                """, Integer.class, slug, currentWeddingId, currentWeddingId);
        return count != null && count > 0;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String normalizeStatus(String status) {
        return "published".equalsIgnoreCase(status) || "active".equalsIgnoreCase(status) ? "active" : "draft";
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private LocalTime parseTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LocalTime.parse(value);
    }

    private LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalTime getLocalTime(ResultSet rs, String column) throws SQLException {
        java.sql.Time time = rs.getTime(column);
        return time == null ? null : time.toLocalTime();
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private String getName(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record TemplateRecord(Long id, String code) {
    }
}
