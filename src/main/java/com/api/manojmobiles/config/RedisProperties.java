package com.api.manojmobiles.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Type-safe configuration properties for Redis TTLs and rate limiting.
 * Bound to the "app.redis" prefix in application.yml.
 *
 * All durations use Spring Boot's Duration parsing (e.g., "2m", "7d", "1h").
 * No TTL value is hardcoded — everything is externalized.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {

    /** OTP expiration time. Default: 2 minutes. */
    private Duration otpTtl = Duration.ofMinutes(2);

    /** Refresh token expiration time. Default: 7 days. */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /** Password reset token expiration time. Default: 10 minutes. */
    private Duration passwordResetTtl = Duration.ofMinutes(10);

    /** Cache TTL configuration. */
    private Cache cache = new Cache();

    /** Rate limiting configuration. */
    private Rate rate = new Rate();

    @Getter
    @Setter
    public static class Cache {
        /** Product cache TTL. Default: 1 hour. */
        private Duration productTtl = Duration.ofHours(1);

        /** Category cache TTL. Default: 6 hours. */
        private Duration categoryTtl = Duration.ofHours(6);
    }

    @Getter
    @Setter
    public static class Rate {
        /** Max login attempts within the window. */
        private int loginLimit = 5;

        /** Login rate limit window. Default: 10 minutes. */
        private Duration loginWindow = Duration.ofMinutes(10);

        /** Max OTP requests within the window. */
        private int otpLimit = 3;

        /** OTP rate limit window. Default: 10 minutes. */
        private Duration otpWindow = Duration.ofMinutes(10);

        /** Max forgot-password requests within the window. */
        private int forgotPasswordLimit = 3;

        /** Forgot-password rate limit window. Default: 1 hour. */
        private Duration forgotPasswordWindow = Duration.ofHours(1);
    }
}
