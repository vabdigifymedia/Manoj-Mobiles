package com.api.manojmobiles.dto.deliverypartner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryPartnerResponseDTO {
    private UUID id;
    private String name;
    private String phone;
    private String vehicleNo;
    private Boolean isActive;
    private java.time.LocalDateTime createdAt;
}
