package com.shortify.api.dto;


import lombok.AllArgsConstructor;
import lombok.Data;


public class ShortenResponse {
    private String shortUrl;
    private String originalUrl;

    public ShortenResponse(String shortUrl, String originalUrl) {
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }
}
