package com.api.manojmobiles.service;

import com.api.manojmobiles.config.RedisProperties;
import com.api.manojmobiles.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * OTP service backed by Redis using StringRedisTemplate directly.
 * Does NOT use @Cacheable — uses explicit Redis operations for full control.
 *
 * Key pattern: otp:{phone}
 * TTL: configurable via app.redis.otp-ttl (default 2 minutes)
 *
 * Guarantees:
 * - One OTP per phone at a time (previous OTP is replaced)
 * - OTP is deleted after successful verification (prevents reuse)
 * - OTP expires automatically via Redis TTL
 *
 * Security:
 * - OTP values are NEVER logged
 * - Uses SecureRandom for OTP generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_KEY_PREFIX = "otp:";
    private static final int OTP_LENGTH = 6;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisProperties redisProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new OTP for the given phone number.
     * If an OTP already exists for this phone, it is replaced.
     *
     * @param phone the phone number (10 digits)
     * @return the generated OTP (caller should send via SMS)
     */
    public String generateOtp(String phone) {
        String otp = generateSecureOtp();
        String key = OTP_KEY_PREFIX + phone;

        try {
            // SET with TTL — replaces any existing OTP for this phone
            stringRedisTemplate.opsForValue().set(
                    key,
                    otp,
                    redisProperties.getOtpTtl());
            log.info("OTP generated for phone:{}", maskPhone(phone));
        } catch (Exception e) {
            log.error("Failed to store OTP in Redis for phone:{}. Error: {}", maskPhone(phone), e.getMessage());
            throw new RuntimeException("OTP service is temporarily unavailable. Please try again later.");
        }

        return otp;
    }

    /**
     * Verify the OTP for the given phone number.
     * On success, the OTP is deleted from Redis (single-use).
     *
     * @param phone the phone number
     * @param otp   the OTP to verify
     * @return true if OTP is valid
     * @throws BadRequestException if OTP is invalid or expired
     */
    public boolean verifyOtp(String phone, String otp) {
        String key = OTP_KEY_PREFIX + phone;

        try {
            String storedOtp = stringRedisTemplate.opsForValue().get(key);

            if (storedOtp == null) {
                log.warn("OTP verification failed for phone:{} — OTP expired or not found", maskPhone(phone));
                throw new BadRequestException("OTP has expired or was not requested. Please request a new OTP.");
            }

            if (!storedOtp.equals(otp)) {
                log.warn("OTP verification failed for phone:{} — invalid OTP provided", maskPhone(phone));
                throw new BadRequestException("Invalid OTP. Please check and try again.");
            }

            // OTP is valid — delete to prevent reuse
            stringRedisTemplate.delete(key);
            log.info("OTP verified successfully for phone:{}", maskPhone(phone));
            return true;

        } catch (BadRequestException e) {
            throw e; // re-throw domain exceptions
        } catch (Exception e) {
            log.error("Failed to verify OTP in Redis for phone:{}. Error: {}", maskPhone(phone), e.getMessage());
            throw new RuntimeException("OTP verification service is temporarily unavailable. Please try again later.");
        }
    }

    /**
     * Generate a cryptographically secure numeric OTP.
     */
    private String generateSecureOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int otpValue = secureRandom.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", otpValue);
    }

    /**
     * Mask phone number for safe logging (e.g., "9876543210" → "98****3210").
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6)
            return "****";
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 4);
    }
}
