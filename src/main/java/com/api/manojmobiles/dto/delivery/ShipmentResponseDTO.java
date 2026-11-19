package com.api.manojmobiles.dto.delivery;

import com.api.manojmobiles.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponseDTO {
    private UUID id;
    private UUID orderId;
    private String orderNumber;
    private DeliveryAgentResponseDTO agent;
    private OrderStatus currentStatus;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedAt;
    private LocalDateTime deliveredAt;
    private String routePolyline;
    private Integer routeDistanceMetres;
    private Integer routeDurationSeconds;
    private Double progress;
    private Integer remainingMetres;
    private Integer remainingSeconds;
    private LocalDateTime eta;

    // Rider order & delivery details
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String deliveryPincode;
    private Double destinationLat;
    private Double destinationLng;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal collectAmount;
    private String storeName;
    private String storeAddress;
    private Double storeLat;
    private Double storeLng;
}
