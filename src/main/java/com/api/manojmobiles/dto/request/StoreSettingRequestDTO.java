package com.api.manojmobiles.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StoreSettingRequestDTO {
    @NotBlank(message = "Store name is required")
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
}
