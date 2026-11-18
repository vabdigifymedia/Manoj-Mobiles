package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.delivery.RouteInfoDTO;
import com.api.manojmobiles.utils.GeoUtils;
import com.api.manojmobiles.utils.PolylineCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Service
@Slf4j
public class GoogleMapsService {

    /** Conservative city two-wheeler speed (~22 km/h) used for fallback & ping-to-ping ETA updates. */
    private static final double DEFAULT_CITY_SPEED_MPS = 22.0 / 3.6;
    /** Road distance is typically ~1.3x straight-line distance in dense Indian cities. */
    private static final double ROAD_CIRCUITY_FACTOR = 1.3;
    /** Preparation / handover buffer in seconds for fallback ETA. */
    private static final int FALLBACK_BUFFER_SECONDS = 300;
    private static final int FALLBACK_SEGMENTS = 12;

    @Value("${google.maps.api-key:mock-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches a real driving route (encoded polyline + traffic-aware duration) from Google Directions API.
     * Falls back cleanly to a straight-line interpolated route when the API key is missing, set to mock-key,
     * or if the external call fails.
     */
    public RouteInfoDTO getRoute(Double originLat, Double originLng, Double destLat, Double destLng) {
        if (originLat == null || originLng == null || destLat == null || destLng == null) {
            log.info("Route coordinates incomplete; returning default 30-min fallback ETA without polyline");
            return RouteInfoDTO.builder()
                    .encodedPolyline("")
                    .distanceMetres(0)
                    .durationSeconds(1800)
                    .eta(LocalDateTime.now().plusMinutes(30))
                    .fallback(true)
                    .build();
        }

        if (hasUsableApiKey()) {
            try {
                String url = UriComponentsBuilder
                        .fromUriString("https://maps.googleapis.com/maps/api/directions/json")
                        .queryParam("origin", originLat + "," + originLng)
                        .queryParam("destination", destLat + "," + destLng)
                        .queryParam("mode", "driving")
                        .queryParam("departure_time", "now")
                        .queryParam("key", apiKey)
                        .toUriString();

                String body = restTemplate.getForObject(url, String.class);
                if (body != null) {
                    JsonNode root = objectMapper.readTree(body);
                    String status = root.path("status").asText("");
                    if ("OK".equals(status) && root.path("routes").isArray() && !root.path("routes").isEmpty()) {
                        JsonNode route = root.path("routes").get(0);
                        String polyline = route.path("overview_polyline").path("points").asText("");
                        JsonNode leg = route.path("legs").isArray() && !route.path("legs").isEmpty()
                                ? route.path("legs").get(0)
                                : null;

                        int distanceMetres = leg != null ? leg.path("distance").path("value").asInt(0) : 0;
                        int durationSeconds = 0;
                        if (leg != null) {
                            durationSeconds = leg.path("duration_in_traffic").path("value").asInt(0);
                            if (durationSeconds <= 0) {
                                durationSeconds = leg.path("duration").path("value").asInt(0);
                            }
                        }
                        if (durationSeconds <= 0) {
                            durationSeconds = estimateRemainingSeconds(distanceMetres, null);
                        }

                        return RouteInfoDTO.builder()
                                .encodedPolyline(polyline)
                                .distanceMetres(distanceMetres)
                                .durationSeconds(durationSeconds)
                                .eta(LocalDateTime.now().plusSeconds(durationSeconds))
                                .fallback(false)
                                .build();
                    }
                    log.warn("Google Directions API returned status={}; falling back to straight-line route", status);
                }
            } catch (Exception ex) {
                log.warn("Google Directions API call failed ({}); falling back to straight-line route", ex.getMessage());
            }
        } else {
            log.debug("Google Maps API key not configured; using straight-line fallback route");
        }

        return buildStraightLineFallback(originLat, originLng, destLat, destLng);
    }

    /**
     * Backward-compatible ETA helper used by checkout and partner assignment.
     */
    public LocalDateTime calculateETA(Double storeLat, Double storeLng, Double userLat, Double userLng) {
        return getRoute(storeLat, storeLng, userLat, userLng).getEta();
    }

    /**
     * Dynamically estimates remaining seconds from remaining distance along the polyline
     * without hitting Directions API on every 3-second GPS ping.
     */
    public int estimateRemainingSeconds(double remainingMetres, Double speedKmh) {
        if (remainingMetres <= 25.0) {
            return 0;
        }
        double speedMps = DEFAULT_CITY_SPEED_MPS;
        if (speedKmh != null && speedKmh >= 5.0 && speedKmh <= 80.0) {
            speedMps = speedKmh / 3.6;
        }
        return (int) Math.max(30, Math.round(remainingMetres / speedMps));
    }

    private RouteInfoDTO buildStraightLineFallback(double originLat, double originLng, double destLat, double destLng) {
        double straightMetres = GeoUtils.haversineMetres(originLat, originLng, destLat, destLng);
        int estimatedRoadMetres = (int) Math.round(straightMetres * ROAD_CIRCUITY_FACTOR);
        int durationSeconds = (int) Math.round(estimatedRoadMetres / DEFAULT_CITY_SPEED_MPS) + FALLBACK_BUFFER_SECONDS;

        double[][] points = new double[FALLBACK_SEGMENTS + 1][2];
        for (int i = 0; i <= FALLBACK_SEGMENTS; i++) {
            double t = (double) i / FALLBACK_SEGMENTS;
            points[i][0] = originLat + (destLat - originLat) * t;
            points[i][1] = originLng + (destLng - originLng) * t;
        }

        return RouteInfoDTO.builder()
                .encodedPolyline(PolylineCodec.encode(points))
                .distanceMetres(estimatedRoadMetres)
                .durationSeconds(durationSeconds)
                .eta(LocalDateTime.now().plusSeconds(durationSeconds))
                .fallback(true)
                .build();
    }

    private boolean hasUsableApiKey() {
        return apiKey != null && !apiKey.isBlank() && !"mock-key".equalsIgnoreCase(apiKey.trim());
    }
}
