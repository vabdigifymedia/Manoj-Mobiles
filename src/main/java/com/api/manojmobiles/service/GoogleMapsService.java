package com.api.manojmobiles.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class GoogleMapsService {

    @Value("${google.maps.api-key:mock-key}")
    private String apiKey;

    public LocalDateTime calculateETA(Double storeLat, Double storeLng, Double userLat, Double userLng) {
        if ("mock-key".equals(apiKey) || storeLat == null || storeLng == null || userLat == null || userLng == null) {
            log.info("Using mock ETA (30 mins) because API key is missing or coordinates are null");
            return LocalDateTime.now().plusMinutes(30);
        }

        // Ideally, we would call the Google Maps Distance Matrix API here:
        // String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + storeLat + "," + storeLng + "&destinations=" + userLat + "," + userLng + "&key=" + apiKey;
        // String response = restTemplate.getForObject(url, String.class);
        // parse JSON to get duration in seconds, then return LocalDateTime.now().plusSeconds(duration)
        
        log.info("Mocking Google Maps API call with real coordinates");
        return LocalDateTime.now().plusMinutes(45);
    }
}
