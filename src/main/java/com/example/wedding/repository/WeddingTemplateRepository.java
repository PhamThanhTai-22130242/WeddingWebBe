package com.example.wedding.repository;

import com.example.wedding.entity.WeddingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface WeddingTemplateRepository extends JpaRepository<WeddingTemplate, Long> {
    Optional<WeddingTemplate> findByCode(String code);
    List<WeddingTemplate> findByIsHiddenFalse();
}
