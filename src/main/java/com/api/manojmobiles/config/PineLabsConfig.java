package com.api.manojmobiles.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pine Labs (Plural) API configuration.
 * 
 * NOTE: The official pinelabs-java SDK (v0.1.0) has a packaging bug where all classes
 * are in the default (unnamed) package, making them unusable from named Java packages.
 * We use direct REST API calls via RestTemplate instead.
 * 
 * Implements auto-refreshing OAuth2 token caching with the client_credentials grant.
 */
@Slf4j
@Configuration
public class PineLabsConfig {

    @Value("${app.payment.pine-labs.client-id}")
    private String clientId;

    @Value("${app.payment.pine-labs.client-secret}")
    private String clientSecret;

    @Value("${app.payment.pine-labs.base-url}")
    private String baseUrl;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    @Bean
    public RestTemplate pineLabsRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Returns a valid access token, refreshing it if needed.
     * Thread-safe with double-checked locking.
     */
    public String getAccessToken() {
        // Check if token is still valid (with 30s buffer)
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
            return cachedToken;
        }

        lock.lock();
        try {
            // Double-check after acquiring lock
            if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
                return cachedToken;
            }

            log.info("Fetching new Pine Labs access token...");

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "client_credentials");
            body.put("client_id", clientId);
            body.put("client_secret", clientSecret);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/api/auth/v2/token",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new RuntimeException("Failed to get Pine Labs access token: " + responseBody);
            }

            cachedToken = responseBody.get("access_token").toString();
            int expiresIn = responseBody.containsKey("expires_in") 
                    ? Integer.parseInt(responseBody.get("expires_in").toString()) 
                    : 3600;
            expiresAt = Instant.now().plusSeconds(expiresIn);

            log.info("Pine Labs access token refreshed, expires at {}", expiresAt);
            return cachedToken;

        } finally {
            lock.unlock();
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
