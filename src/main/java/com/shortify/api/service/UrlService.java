package com.shortify.api.service;


import com.shortify.api.dto.ShortenRequest;
import com.shortify.api.dto.ShortenResponse;
import com.shortify.api.entity.Url;
import com.shortify.api.kafka.ClickEventProducer;
import com.shortify.api.repository.UrlRepository;
import com.shortify.api.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.util.concurrent.TimeUnit;

@Service

public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final RedisTemplate<String, String> redisTemplate;

    private final ClickEventProducer clickEventProducer;
    @Value("${app.base-url}")
    private String baseUrl;

    private static final String REDIS_PREFIX = "url:";
    private static final long CACHE_TTL_HOURS = 24;

    public UrlService(UrlRepository urlRepository, Base62Encoder base62Encoder, RedisTemplate<String, String> redisTemplate, ClickEventProducer clickEventProducer) {
        this.urlRepository = urlRepository;
        this.base62Encoder = base62Encoder;
        this.redisTemplate = redisTemplate;
        this.clickEventProducer = clickEventProducer;
    }

    // POST /api/shorten
    public ShortenResponse shortenUrl(ShortenRequest request) {
        // Save to MySQL first to get auto-increment ID
        Url url = new Url();
        url.setOriginalUrl(request.getUrl());
        url.setShortCode("temp"); // temporary
        Url saved = urlRepository.save(url);

        // Generate short code from ID using Base62
        String shortCode = base62Encoder.encode(saved.getId());
        saved.setShortCode(shortCode);
        urlRepository.save(saved);

        // Cache in Redis with TTL
        redisTemplate.opsForValue().set(
                REDIS_PREFIX + shortCode,
                request.getUrl(),
                CACHE_TTL_HOURS,
                TimeUnit.HOURS
        );

        return new ShortenResponse(baseUrl + "/" + shortCode, request.getUrl());
    }

    // GET /{shortCode} — redirect
    public String getOriginalUrl(String shortCode) {
        // 1. Check Redis first (Cache-aside pattern)
        String cached = redisTemplate.opsForValue().get(REDIS_PREFIX + shortCode);
        if (cached != null) {
            clickEventProducer.publishClickEvent(shortCode);
            return cached;
        }

        // 2. Cache miss — fetch from MySQL
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("Short URL not found: " + shortCode));

        // 3. Store back in Redis for next time
        redisTemplate.opsForValue().set(
                REDIS_PREFIX + shortCode,
                url.getOriginalUrl(),
                CACHE_TTL_HOURS,
                TimeUnit.HOURS
        );

        clickEventProducer.publishClickEvent(shortCode);
        return url.getOriginalUrl();
    }

    // GET /api/stats/{shortCode}
    public Url getStats(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("Short URL not found: " + shortCode));
    }

    @Transactional
    public void incrementClick(String shortCode) {
        urlRepository.incrementClickCount(shortCode);
    }
}
