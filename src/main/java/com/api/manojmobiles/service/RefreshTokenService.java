package com.api.manojmobiles.service;

import com.api.manojmobiles.config.RedisProperties;
import com.api.manojmobiles.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Refresh token service backed by Redis.
 *
 * Key pattern: refresh:{userId} → stores the refresh token string
 * TTL: configurable via app.redis.refresh-token-ttl (default 7 days)
 *
 * Design decisions:
 * - Access token remains stateless (JWT only) — NOT stored in Redis
 * - Refresh token IS stored in Redis for server-side validation & revocation
 * - One refresh token per user (new login replaces old refresh token)
 * - Logout deletes the refresh token from Redis
 *
 * Security:
 * - Refresh token values are NEVER logged
 * - Invalid tokens return proper 401 authentication errors
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisProperties redisProperties;

    /**
     * Create a new refresh token for the given user.
     * Replaces any existing refresh token for this user.
     *
     * @param userId the user's UUID
     * @return the generated refresh token string
     */
    public String createRefreshToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        String key = REFRESH_KEY_PREFIX + userId;

        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    token,
                    redisProperties.getRefreshTokenTtl());
            log.info("Refresh token created for userId:{}", userId);
        } catch (Exception e) {
            log.error("Failed to store refresh token in Redis for userId:{}. Error: {}", userId, e.getMessage());
            throw new RuntimeException("Authentication service is temporarily unavailable. Please try again later.");
        }

        return token;
    }

    /**
     * Validate a refresh token for the given user.
     *
     * @param userId       the user's UUID
     * @param refreshToken the refresh token to validate
     * @throws BadRequestException if the token is invalid, expired, or doesn't
     *                             match
     */
    public void validateRefreshToken(UUID userId, String refreshToken) {
        String key = REFRESH_KEY_PREFIX + userId;

        try {
            String storedToken = stringRedisTemplate.opsForValue().get(key);

            if (storedToken == null) {
                log.warn("Refresh token validation failed for userId:{} — token expired or not found", userId);
                throw new BadRequestException("Refresh token has expired. Please log in again.");
            }

            if (!storedToken.equals(refreshToken)) {
                log.warn("Refresh token validation failed for userId:{} — token mismatch", userId);
                throw new BadRequestException("Invalid refresh token. Please log in again.");
            }

            log.debug("Refresh token validated successfully for userId:{}", userId);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate refresh token in Redis for userId:{}. Error: {}", userId, e.getMessage());
            throw new BadRequestException("Authentication service is temporarily unavailable. Please log in again.");
        }
    }

    /**
     * Delete the refresh token for the given user (logout).
     *
     * @param userId the user's UUID
     */
    public void deleteRefreshToken(UUID userId) {
        String key = REFRESH_KEY_PREFIX + userId;

        try {
            Boolean deleted = stringRedisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Refresh token deleted for userId:{} (logout)", userId);
            } else {
                log.debug("No refresh token found to delete for userId:{}", userId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete refresh token from Redis for userId:{}. Error: {}", userId, e.getMessage());
            // Don't throw — logout should succeed even if Redis is down
        }
    }
}
