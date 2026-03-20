package com.shortify.api.service;



import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service

public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String RATE_PREFIX = "rate::";

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String ipAddress) {
        String key = RATE_PREFIX + ipAddress;

        // Increment request count for this IP
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            log.warn("Redis returned null for key: {}", key);
            return true; // fail open
        }

        // First request — set expiry of 1 minute
        if (count == 1) {
            redisTemplate.expire(key, WINDOW);
            log.info("Rate limit window started for IP: {}", ipAddress);
        }

        log.info("Request count for IP {}: {}/{}", ipAddress, count, MAX_REQUESTS);

        return count <= MAX_REQUESTS;
    }
}
