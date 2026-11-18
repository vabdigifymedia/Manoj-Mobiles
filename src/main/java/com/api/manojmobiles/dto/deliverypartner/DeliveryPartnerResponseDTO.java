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
    private String email;
    private String vehicleNo;
    private Boolean isActive;
    private UUID userId;
    private UUID agentId;
    private String loginUsername; // Rider logs in with this (email or phone)
    private java.time.LocalDateTime createdAt;
}
