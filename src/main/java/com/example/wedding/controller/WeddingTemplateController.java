package com.example.wedding.controller;

import com.example.wedding.config.APIResponse;
import com.example.wedding.config.ResponseStatus;
import com.example.wedding.entity.WeddingTemplate;
import com.example.wedding.service.WeddingTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
public class WeddingTemplateController {
    private final WeddingTemplateService templateService;

    public WeddingTemplateController(WeddingTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/api/templates")
    public ResponseEntity<APIResponse<List<WeddingTemplate>>> getPublicTemplates() {
        List<WeddingTemplate> templates = templateService.getPublicTemplates();
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, templates));
    }

    @GetMapping("/api/admin/templates")
    public ResponseEntity<APIResponse<List<WeddingTemplate>>> getAdminTemplates(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        List<WeddingTemplate> templates = templateService.getAdminTemplates(authorizationHeader);
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS, templates));
    }

    @PutMapping("/api/admin/templates/{code}")
    public ResponseEntity<APIResponse<WeddingTemplate>> updateTemplate(
            @PathVariable String code,
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        String price = (String) request.get("price");
        String promoPrice = (String) request.get("promoPrice");
        
        Integer category = null;
        Object catObj = request.get("category");
        if (catObj instanceof Number) {
            category = ((Number) catObj).intValue();
        } else if (catObj instanceof String) {
            try {
                category = Integer.parseInt((String) catObj);
            } catch (NumberFormatException ignored) {}
        }

        WeddingTemplate response = templateService.updateTemplate(
                authorizationHeader,
                code,
                price,
                promoPrice,
                category
        );
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS.getCode(), "Cập nhật mẫu thiệp thành công", response));
    }

    @PatchMapping("/api/admin/templates/{code}/visibility")
    public ResponseEntity<APIResponse<WeddingTemplate>> toggleTemplateVisibility(
            @PathVariable String code,
            @RequestBody Map<String, Boolean> request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        Boolean isHidden = request != null && request.get("isHidden") != null ? request.get("isHidden") : false;
        WeddingTemplate response = templateService.toggleTemplateVisibility(
                authorizationHeader,
                code,
                isHidden
        );
        return ResponseEntity.ok(new APIResponse<>(ResponseStatus.SUCCESS.getCode(), "Cập nhật trạng thái hiển thị thành công", response));
    }
}
