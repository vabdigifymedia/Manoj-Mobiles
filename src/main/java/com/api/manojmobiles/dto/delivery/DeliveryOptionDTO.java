package com.api.manojmobiles.dto.delivery;

import com.api.manojmobiles.entity.enums.DeliveryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOptionDTO {
    private DeliveryType type;
    private String label;
    private String description;
    private boolean available;
    private String unavailableReason;
    private BigDecimal deliveryCharge;
    private Integer estimatedMinutes;
    private Integer estimatedDays;
    private LocalDateTime estimatedDeliveryTime;
    private boolean liveTrackingSupported;
    private boolean codAvailable;
}
