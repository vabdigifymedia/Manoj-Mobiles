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
 * Key pattern: refresh:{userId}:{token} → stores "active"
 * TTL: configurable via app.redis.refresh-token-ttl (default 7 days)
 *
 * Design decisions:
 * - Access token remains stateless (JWT only) — NOT stored in Redis
 * - Refresh token IS stored in Redis for server-side validation & revocation
 * - MULTI-DEVICE SUPPORT: A user can have multiple active refresh tokens
 * - Logout deletes the specific refresh token from Redis
 * - Global Logout (e.g., password change) deletes all tokens for that user
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
        String key = REFRESH_KEY_PREFIX + userId + ":" + token;

        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    "active",
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
        String key = REFRESH_KEY_PREFIX + userId + ":" + refreshToken;

        try {
            String status = stringRedisTemplate.opsForValue().get(key);

            if (status == null) {
                log.warn("Refresh token validation failed for userId:{} — token expired or not found", userId);
                throw new BadRequestException("Refresh token has expired or is invalid. Please log in again.");
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
     * Delete a specific refresh token (single device logout).
     */
    public void deleteRefreshToken(UUID userId, String refreshToken) {
        String key = REFRESH_KEY_PREFIX + userId + ":" + refreshToken;
        try {
            Boolean deleted = stringRedisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Specific refresh token deleted for userId:{} (logout)", userId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete specific refresh token for userId:{}. Error: {}", userId, e.getMessage());
        }
    }

    /**
     * Delete ALL refresh tokens for the given user (global logout / password change).
     */
    public void deleteAllRefreshTokens(UUID userId) {
        String pattern = REFRESH_KEY_PREFIX + userId + ":*";
        try {
            var keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.info("Deleted {} refresh tokens for userId:{} (global logout)", keys.size(), userId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete all refresh tokens for userId:{}. Error: {}", userId, e.getMessage());
        }
    }
}
