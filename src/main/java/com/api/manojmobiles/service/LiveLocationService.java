package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.order.LiveLocationResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveLocationService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String LOCATION_KEY_PREFIX = "order:live-location:";

    public void updateLiveLocation(UUID orderId, Double lat, Double lng) {
        String key = LOCATION_KEY_PREFIX + orderId;
        String value = lat + "," + lng;
        
        // Store location with a TTL of 2 hours
        redisTemplate.opsForValue().set(key, value, Duration.ofHours(2));
        log.info("Updated live location for order {}: {}, {}", orderId, lat, lng);
    }

    public LiveLocationResponseDTO getLiveLocation(UUID orderId) {
        String key = LOCATION_KEY_PREFIX + orderId;
        String value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            String[] parts = value.split(",");
            if (parts.length == 2) {
                return LiveLocationResponseDTO.builder()
                        .lat(Double.parseDouble(parts[0]))
                        .lng(Double.parseDouble(parts[1]))
                        .build();
            }
        }
        
        return null;
    }
}
