package com.shortify.api.controller;

import com.shortify.api.dto.ShortenRequest;
import com.shortify.api.dto.ShortenResponse;
import com.shortify.api.entity.Url;
import com.shortify.api.exception.RateLimitExceededException;
import com.shortify.api.service.RateLimiterService;
import com.shortify.api.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;

    private final RateLimiterService rateLimiterService;

    public UrlController(UrlService urlService, RateLimiterService rateLimiterService) {
        this.urlService = urlService;
        this.rateLimiterService = rateLimiterService;
    }

    // Shorten a URL — rate limited
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        log.info("Shorten request from IP: {}", clientIp);

        // Check rate limit
        if (!rateLimiterService.isAllowed(clientIp)) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Max 5 requests per minute allowed."
            );
        }

        ShortenResponse response = urlService.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Redirect to original URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = urlService.getOriginalUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    // Get stats
    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<Url> stats(@PathVariable String shortCode) {
        Url url = urlService.getStats(shortCode);
        return ResponseEntity.ok(url);
    }
}