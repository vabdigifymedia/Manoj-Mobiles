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

    @Id
    @Column(length = 50)
    private String id = "DEFAULT_CONFIG";

    private String storeName = "Manoj Mobiles";
    private String announcementText = "Free delivery on orders above ₹999 · Easy 7-day returns";
    private String announcementLink;
    private Boolean announcementActive = true;

    private String whatsappNumber = "+919876543210";
    private String whatsappDefaultMessage = "Hi Manoj Mobiles, I need help choosing a smartphone.";
    private String supportPhone = "+919876543210";
    private String supportEmail = "support@manojmobiles.com";

    @Column(columnDefinition = "TEXT")
    private String storeAddress = "Main Market Road, Near City Center";
    private String storeTimings = "10:00 AM - 9:30 PM (All 7 Days)";
    private String googleMapsUrl;

    private BigDecimal freeDeliveryThreshold = BigDecimal.valueOf(999.00);
    private String expressDeliveryText = "Get delivery within 2 hours in selected pin codes";

    @UpdateTimestamp
    private Instant updatedAt;
}
