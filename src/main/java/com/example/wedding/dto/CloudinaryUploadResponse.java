package com.example.wedding.dto;

public class CloudinaryUploadResponse {
    private final String publicId;
    private final String url;
    private final String secureUrl;
    private final String resourceType;
    private final String format;
    private final Long bytes;
    private final Integer width;
    private final Integer height;

    public CloudinaryUploadResponse(
            String publicId,
            String url,
            String secureUrl,
            String resourceType,
            String format,
            Long bytes,
            Integer width,
            Integer height
    ) {
        this.publicId = publicId;
        this.url = url;
        this.secureUrl = secureUrl;
        this.resourceType = resourceType;
        this.format = format;
        this.bytes = bytes;
        this.width = width;
        this.height = height;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getUrl() {
        return url;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getFormat() {
        return format;
    }

    public Long getBytes() {
        return bytes;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }
}
