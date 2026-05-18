package com.example.wedding.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.wedding.dto.CloudinaryUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;
    private final String folder;
    private final String cloudName;
    private final String uploadPreset;

    public CloudinaryService(
            Cloudinary cloudinary,
            @Value("${cloudinary.folder:wedding}") String folder,
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.upload-preset}") String uploadPreset
    ) {
        this.cloudinary = cloudinary;
        this.folder = folder;
        this.cloudName = cloudName;
        this.uploadPreset = uploadPreset;
    }

    public CloudinaryUploadResponse upload(MultipartFile file) {
        validateCloudinaryConfig();
        validateFile(file);

        try {
            Map<?, ?> result = cloudinary.uploader().unsignedUpload(file.getBytes(), uploadPreset, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"
            ));

            return new CloudinaryUploadResponse(
                    asString(result.get("public_id")),
                    asString(result.get("url")),
                    asString(result.get("secure_url")),
                    asString(result.get("resource_type")),
                    asString(result.get("format")),
                    asLong(result.get("bytes")),
                    asInteger(result.get("width")),
                    asInteger(result.get("height"))
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("Khong the doc file upload", exception);
        }
    }

    private void validateCloudinaryConfig() {
        if (!StringUtils.hasText(cloudName) || !StringUtils.hasText(uploadPreset)) {
            throw new IllegalArgumentException("Cloudinary chua duoc cau hinh. Vui long set CLOUDINARY_CLOUD_NAME va CLOUDINARY_UPLOAD_PRESET");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui long chon file can upload");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Dinh dang file khong duoc ho tro");
        }
    }

    private boolean isAllowedContentType(String contentType) {
        return contentType.startsWith("image/");
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
