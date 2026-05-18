package com.example.wedding.dto;

public class GoogleTokenInfo {
    private final String sub;
    private final String email;
    private final String name;
    private final String picture;

    public GoogleTokenInfo(String sub, String email, String name, String picture) {
        this.sub = sub;
        this.email = email;
        this.name = name;
        this.picture = picture;
    }

    public String getSub() {
        return sub;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPicture() {
        return picture;
    }
}
