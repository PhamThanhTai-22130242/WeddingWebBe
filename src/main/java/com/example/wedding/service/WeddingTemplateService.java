package com.example.wedding.service;

import com.example.wedding.entity.WeddingTemplate;
import com.example.wedding.exception.NotFoundException;
import com.example.wedding.repository.WeddingTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WeddingTemplateService {
    private final WeddingTemplateRepository templateRepository;
    private final AccessTokenUserService accessTokenUserService;

    public WeddingTemplateService(WeddingTemplateRepository templateRepository, AccessTokenUserService accessTokenUserService) {
        this.templateRepository = templateRepository;
        this.accessTokenUserService = accessTokenUserService;
    }

    @Transactional(readOnly = true)
    public List<WeddingTemplate> getPublicTemplates() {
        return templateRepository.findByIsHiddenFalse();
    }

    @Transactional(readOnly = true)
    public List<WeddingTemplate> getAdminTemplates(String authorizationHeader) {
        accessTokenUserService.requireAdmin(authorizationHeader, "Không có quyền truy cập");
        return templateRepository.findAll();
    }

    @Transactional
    public WeddingTemplate updateTemplate(String authorizationHeader, String code, String price, String promoPrice, Integer category) {
        accessTokenUserService.requireAdmin(authorizationHeader, "Không có quyền quản lý");
        WeddingTemplate template = templateRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mẫu thiệp: " + code));
        
        if (price != null) template.setPrice(price);
        if (promoPrice != null) template.setPromoPrice(promoPrice);
        if (category != null) template.setCategory(category);
        
        return templateRepository.save(template);
    }

    @Transactional
    public WeddingTemplate toggleTemplateVisibility(String authorizationHeader, String code, Boolean isHidden) {
        accessTokenUserService.requireAdmin(authorizationHeader, "Không có quyền quản lý");
        WeddingTemplate template = templateRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mẫu thiệp: " + code));
        
        template.setIsHidden(isHidden);
        return templateRepository.save(template);
    }
}
