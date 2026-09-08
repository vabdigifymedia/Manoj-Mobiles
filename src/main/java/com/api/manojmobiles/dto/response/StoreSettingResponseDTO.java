package com.api.manojmobiles.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class StoreSettingResponseDTO {
    private String id;
    private String storeName;
    private String announcementText;
    private String announcementLink;
    private Boolean announcementActive;
    private String whatsappNumber;
    private String whatsappDefaultMessage;
    private String supportPhone;
    private String supportEmail;
    private String storeAddress;
    private String storeTimings;
    private String googleMapsUrl;
    private BigDecimal freeDeliveryThreshold;
    private String expressDeliveryText;
    private Double storeLat;
    private Double storeLng;
    private Instant updatedAt;
}
