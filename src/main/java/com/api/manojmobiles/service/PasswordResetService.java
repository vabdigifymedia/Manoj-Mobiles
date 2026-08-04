package com.api.manojmobiles.service;

import com.api.manojmobiles.config.RedisProperties;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Password reset token service backed by Redis.
 *
 * Key pattern: password-reset:{token} → stores the user's email
 * TTL: configurable via app.redis.password-reset-ttl (default 10 minutes)
 *
 * Flow:
 * 1. User requests password reset → token generated, stored in Redis, returned
 * (or sent via email)
 * 2. User submits token + new password → token validated, password updated,
 * token deleted
 *
 * Security:
 * - Token values are NEVER logged
 * - Tokens are single-use (deleted after successful reset)
 * - Tokens expire automatically via Redis TTL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String RESET_KEY_PREFIX = "password-reset:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisProperties redisProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a password reset token for the given email.
     * If a token already exists, it is replaced.
     *
     * @param email the user's email
     * @return the generated token (caller should send via email in production)
     */
    public String createResetToken(String email) {
        // Verify user exists
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));

        String token = UUID.randomUUID().toString();
        String key = RESET_KEY_PREFIX + token;

        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    email,
                    redisProperties.getPasswordResetTtl());
            log.info("Password reset token created for email:{}", maskEmail(email));
        } catch (Exception e) {
            log.error("Failed to store password reset token in Redis. Error: {}", e.getMessage());
            throw new RuntimeException("Password reset service is temporarily unavailable. Please try again later.");
        }

        return token;
    }

    /**
     * Reset the user's password using the provided token.
     * Token is validated and deleted after successful password change.
     *
     * @param token       the password reset token
     * @param newPassword the new password (plain text — will be encoded)
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String key = RESET_KEY_PREFIX + token;

        try {
            String email = stringRedisTemplate.opsForValue().get(key);

            if (email == null) {
                log.warn("Password reset failed — token expired or invalid");
                throw new BadRequestException(
                        "Password reset token has expired or is invalid. Please request a new one.");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for this reset token."));

            // Update password
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Delete token — single use
            stringRedisTemplate.delete(key);
            log.info("Password reset successful for email:{}", maskEmail(email));

        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e; // re-throw domain exceptions
        } catch (Exception e) {
            log.error("Failed to process password reset. Error: {}", e.getMessage());
            throw new RuntimeException("Password reset service is temporarily unavailable. Please try again later.");
        }
    }

    /**
     * Mask email for safe logging (e.g., "user@email.com" → "us***@email.com").
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "****";
        String[] parts = email.split("@");
        String name = parts[0];
        String masked = name.length() > 2 ? name.substring(0, 2) + "***" : "***";
        return masked + "@" + parts[1];
    }
}
