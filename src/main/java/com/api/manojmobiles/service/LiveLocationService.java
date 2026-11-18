package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.order.LiveLocationResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveLocationService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCATION_KEY_PREFIX = "order:live-location:";
    private static final String AGENT_LOCATION_KEY_PREFIX = "agent:live-location:";
    private static final Duration ORDER_TTL = Duration.ofHours(2);
    private static final Duration AGENT_TTL = Duration.ofMinutes(15);

    private final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Backward-compatible overload storing bare coordinates for an order.
     */
    public void updateLiveLocation(UUID orderId, Double lat, Double lng) {
        updateLiveLocation(orderId, LiveLocationResponseDTO.builder()
                .lat(lat)
                .lng(lng)
                .rawLat(lat)
                .rawLng(lng)
                .recordedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Stores rich live tracking telemetry (snapped coords, heading, speed, progress, ETA) in Redis.
     */
    public void updateLiveLocation(UUID orderId, LiveLocationResponseDTO location) {
        if (orderId == null || location == null) {
            return;
        }
        String key = LOCATION_KEY_PREFIX + orderId;
        try {
            String json = objectMapper.writeValueAsString(location);
            redisTemplate.opsForValue().set(key, json, ORDER_TTL);
            log.debug("Cached live location for order {}: ({}, {}), progress={}",
                    orderId, location.getLat(), location.getLng(), location.getProgress());
        } catch (Exception ex) {
            log.warn("Failed to cache live location in Redis for order {}: {}", orderId, ex.getMessage());
        }
    }

    /**
     * Reads the latest cached live location for an order from Redis.
     * Supports both the new JSON payload and legacy "lat,lng" entries.
     */
    public LiveLocationResponseDTO getLiveLocation(UUID orderId) {
        if (orderId == null) {
            return null;
        }
        String key = LOCATION_KEY_PREFIX + orderId;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return null;
            }
            if (value.trim().startsWith("{")) {
                return objectMapper.readValue(value, LiveLocationResponseDTO.class);
            }
            // Legacy "lat,lng" fallback
            String[] parts = value.split(",");
            if (parts.length == 2) {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                return LiveLocationResponseDTO.builder()
                        .lat(lat)
                        .lng(lng)
                        .rawLat(lat)
                        .rawLng(lng)
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Failed to read live location from Redis for order {}: {}", orderId, ex.getMessage());
        }
        return null;
    }

    /**
     * Clears the live location key when an order is delivered or cancelled.
     */
    public void clearLiveLocation(UUID orderId) {
        if (orderId == null) {
            return;
        }
        try {
            redisTemplate.delete(LOCATION_KEY_PREFIX + orderId);
        } catch (Exception ex) {
            log.warn("Failed to clear live location from Redis for order {}: {}", orderId, ex.getMessage());
        }
    }

    /**
     * Caches an idle or active delivery agent's heartbeat GPS in Redis for fast nearest-agent lookups.
     */
    public void updateAgentHeartbeat(UUID agentId, Double lat, Double lng, Double heading) {
        if (agentId == null || lat == null || lng == null) {
            return;
        }
        String key = AGENT_LOCATION_KEY_PREFIX + agentId;
        try {
            LiveLocationResponseDTO dto = LiveLocationResponseDTO.builder()
                    .lat(lat)
                    .lng(lng)
                    .rawLat(lat)
                    .rawLng(lng)
                    .heading(heading)
                    .recordedAt(LocalDateTime.now())
                    .build();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(dto), AGENT_TTL);
        } catch (Exception ex) {
            log.warn("Failed to cache agent heartbeat in Redis for agent {}: {}", agentId, ex.getMessage());
        }
    }

    public LiveLocationResponseDTO getAgentHeartbeat(UUID agentId) {
        if (agentId == null) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue().get(AGENT_LOCATION_KEY_PREFIX + agentId);
            if (value != null && value.trim().startsWith("{")) {
                return objectMapper.readValue(value, LiveLocationResponseDTO.class);
            }
        } catch (Exception ex) {
            log.warn("Failed to read agent heartbeat from Redis for agent {}: {}", agentId, ex.getMessage());
        }
        return null;
    }
}
