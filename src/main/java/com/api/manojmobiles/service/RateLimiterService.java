package com.api.manojmobiles.service;

import com.api.manojmobiles.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Distributed Rate Limiter backed by Redis.
 * 
 * Uses atomic Redis INCR and EXPIRE commands.
 * Fails open (allows request) if Redis is unavailable to prevent total system outage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Checks if the given key has exceeded the maximum allowed requests within the given time window.
     * 
     * @param key         the unique identifier for the rate limit (e.g., "rate:login:192.168.1.1")
     * @param maxRequests maximum allowed requests in the window
     * @param window      the time window
     * @throws RateLimitExceededException if the limit is exceeded
     */
    public void checkRateLimit(String key, int maxRequests, Duration window) {
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            
            if (count != null && count == 1) {
                // First request, set the expiration window
                stringRedisTemplate.expire(key, window);
            }
            
            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for key: {}", key);
                throw new RateLimitExceededException("Too many requests. Please try again later.");
            }
        } catch (RateLimitExceededException e) {
            throw e; // re-throw the domain exception
        } catch (Exception e) {
            // Fail open: log the error but allow the request to proceed if Redis is down
            log.error("Failed to check rate limit in Redis for key {}. Error: {}. Failing open.", key, e.getMessage());
        }
    }
}
