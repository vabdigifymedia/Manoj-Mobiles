package com.api.manojmobiles.dto.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentResponseDTO {
    private UUID id;
    private UUID userId;
    private String name;
    private String phone;
    private String vehicleNo;
    private Boolean isAvailable;
    private Double currentLat;
    private Double currentLng;
    private Double heading;
    private LocalDateTime lastLocationAt;
}
