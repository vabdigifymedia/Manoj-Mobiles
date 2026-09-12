package com.api.manojmobiles.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class AiToolsConfig {

    public record MobileSearchRequest(String brand, Double maxPrice, String query) {}
    public record MobileSearchResponse(List<String> products) {}

    @Bean
    @Description("Search for mobile phones in the database by brand, max price, or query string.")
    public Function<MobileSearchRequest, MobileSearchResponse> searchMobiles() {
        return request -> {
            // For now, return some dummy products
            return new MobileSearchResponse(List.of("iPhone 15 Pro", "Samsung S24 Ultra"));
        };
    }
}
