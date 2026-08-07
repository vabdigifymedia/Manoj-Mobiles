package com.api.manojmobiles.dto.delivery;

import com.api.manojmobiles.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ShipmentResponseDTO {
    private UUID id;
    private UUID orderId;
    private String orderNumber;
    private DeliveryAgentResponseDTO agent;
    private OrderStatus currentStatus;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedAt;
    private LocalDateTime deliveredAt;
}
