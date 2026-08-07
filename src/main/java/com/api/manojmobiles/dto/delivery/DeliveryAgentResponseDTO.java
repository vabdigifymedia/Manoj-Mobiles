package com.api.manojmobiles.dto.delivery;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DeliveryAgentResponseDTO {
    private UUID id;
    private UUID userId;
    private String name;
    private String phone;
    private String vehicleNo;
    private Boolean isAvailable;
}
