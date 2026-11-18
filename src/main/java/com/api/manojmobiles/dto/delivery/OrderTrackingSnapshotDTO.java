package com.api.manojmobiles.dto.delivery;

import com.api.manojmobiles.dto.order.LiveLocationResponseDTO;
import com.api.manojmobiles.entity.enums.DeliveryType;
import com.api.manojmobiles.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Complete snapshot returned by GET /api/user/orders/{orderId}/tracking (and admin equivalent)
 * so the frontend map renders store, destination, road polyline, rider location, ETA, progress,
 * and milestone timeline immediately on initial load before the first WebSocket ping arrives.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingSnapshotDTO {
    private UUID orderId;
    private String orderNumber;
    private DeliveryType deliveryType;
    private OrderStatus orderStatus;

    private LocalDateTime placedAt;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime eta;

    /** Courier tracking info for STANDARD deliveries. */
    private String trackingId;
    private String courierPartner;

    private GeoPointDTO store;
    private GeoPointDTO destination;

    private String routePolyline;
    private Integer routeDistanceMetres;
    private Integer routeDurationSeconds;

    private LiveLocationResponseDTO agentLocation;
    private Double progress;
    private Integer remainingMetres;
    private Integer remainingSeconds;

    private DeliveryAgentResponseDTO agent;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedAt;
    private LocalDateTime deliveredAt;

    private boolean liveTrackingActive;
    private String wsEndpoint;
    private String wsUserQueue;
    private String wsOrderTopic;

    private List<StatusTimelineItemDTO> timeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPointDTO {
        private Double lat;
        private Double lng;
        private String label;
        private String address;
        private String pincode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusTimelineItemDTO {
        private OrderStatus status;
        private LocalDateTime changedAt;
        private String note;
    }
}
