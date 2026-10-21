package com.api.manojmobiles.service;

import com.api.manojmobiles.config.RedisProperties;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

// =========================================================================
// [TEMPORARY - PINE LABS APPROVAL]
// Reason: TD Digital SMS credits exhausted; added for PG review team.
// Removal: Delete this class and corresponding endpoints once SMS is restored.
// =========================================================================
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private static final String EMAIL_OTP_KEY_PREFIX = "otp:email:";
    private static final int OTP_LENGTH = 6;
    private static final Duration DEFAULT_EMAIL_OTP_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisProperties redisProperties;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new 6-digit OTP for the given email, save to Redis, and send via EmailService.
     *
     * @param email destination email address
     * @return generated OTP string
     */
    public String generateAndSendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        String otp = generateSecureOtp();
        String key = EMAIL_OTP_KEY_PREFIX + normalizedEmail;

        try {
            Duration ttl = redisProperties.getOtpTtl() != null ? redisProperties.getOtpTtl() : DEFAULT_EMAIL_OTP_TTL;
            stringRedisTemplate.opsForValue().set(key, otp, ttl);

            log.info("Email OTP generated for email:{}. [DEV/REVIEW ONLY] OTP is: {}", maskEmail(normalizedEmail), otp);

            // Send OTP email asynchronously
            emailService.sendOtpEmail(normalizedEmail, otp);

        } catch (Exception e) {
            log.error("Failed to store Email OTP in Redis for email:{}. Error: {}", maskEmail(normalizedEmail), e.getMessage());
            throw new RuntimeException("Email OTP service is temporarily unavailable. Please try again later.");
        }

        return otp;
    }

    /**
     * Verify the OTP provided for the given email address.
     * On success, the OTP is deleted from Redis (single-use guarantee).
     *
     * @param email the user email
     * @param otp the OTP entered by user
     * @return true if valid
     */
    public boolean verifyOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        String key = EMAIL_OTP_KEY_PREFIX + normalizedEmail;

        try {
            String storedOtp = stringRedisTemplate.opsForValue().get(key);

            if (storedOtp == null) {
                log.warn("Email OTP verification failed for email:{} — OTP expired or not found", maskEmail(normalizedEmail));
                throw new BadRequestException("OTP has expired or was not requested. Please request a new OTP.");
            }

            if (!storedOtp.equals(otp)) {
                log.warn("Email OTP verification failed for email:{} — invalid OTP provided", maskEmail(normalizedEmail));
                throw new BadRequestException("Invalid OTP. Please check and try again.");
            }

            // OTP verified — remove to prevent replay attacks
            stringRedisTemplate.delete(key);
            log.info("Email OTP verified successfully for email:{}", maskEmail(normalizedEmail));
            return true;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Email OTP in Redis for email:{}. Error: {}", maskEmail(normalizedEmail), e.getMessage());
            throw new RuntimeException("Email OTP verification service is temporarily unavailable. Please try again later.");
        }
    }

    private String generateSecureOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int otpValue = secureRandom.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", otpValue);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "**" + email.substring(atIndex);
        return email.substring(0, 2) + "****" + email.substring(atIndex);
    }
}
