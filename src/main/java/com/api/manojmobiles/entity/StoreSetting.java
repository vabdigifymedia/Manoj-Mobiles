package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "store_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSetting {

    @Builder.Default
    @Id
    @Column(length = 50)
    private String id = "DEFAULT_CONFIG";

    @Builder.Default
    private String storeName = "Manoj Mobiles";
    @Builder.Default
    private String announcementText = "Free delivery on orders above ₹999 · Easy 7-day returns";
    private String announcementLink;
    @Builder.Default
    private Boolean announcementActive = true;

    @Builder.Default
    private String whatsappNumber = "+919876543210";
    @Builder.Default
    private String whatsappDefaultMessage = "Hi Manoj Mobiles, I need help choosing a smartphone.";
    @Builder.Default
    private String supportPhone = "+919876543210";
    @Builder.Default
    private String supportEmail = "support@manojmobiles.com";

    @Builder.Default
    @Column(columnDefinition = "TEXT")
    private String storeAddress = "Main Market Road, Near City Center";
    @Builder.Default
    private String storeTimings = "10:00 AM - 9:30 PM (All 7 Days)";
    private String googleMapsUrl;

    @Builder.Default
    private BigDecimal freeDeliveryThreshold = BigDecimal.valueOf(999.00);
    @Builder.Default
    private String expressDeliveryText = "Get delivery within 2 hours in selected pin codes";

    @UpdateTimestamp
    private Instant updatedAt;
}
