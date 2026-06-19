package com.example.wedding.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseSchemaService {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute("alter table wishes modify column message varchar(300) null");
        ensureTemplate("EmeraldInvitation", "Hỷ Sắc Vu Quy", "/EmeraldInvitation");
        ensureTemplate("RubyBasicInvitation", "Bến Tình Trăm Năm", "/RubyBasicInvitation");
        ensureTemplate("CineLoveTraditionalInvitation", "Duyên Thắm Miệt Vườn", "/CineLoveTraditionalInvitation");
        ensureTemplate("ElegantInvitation", "Trăm Năm Bến Đợi", "/tram-nam-ben-doi");
        ensureTemplate("PinkWeddingInvitation", "Hoa Hảo Nguyệt Viên", "/THIEPMAUHONG");
    }

    private void ensureTemplate(String code, String name, String previewImg) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from wedding_templates where code = ?",
                Integer.class,
                code
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
                insert into wedding_templates (name, code, preview_img, is_active, created_at, updated_at)
                values (?, ?, ?, 1, current_timestamp(), current_timestamp())
                """, name, code, previewImg);
    }
}
