package com.api.manojmobiles.dto.delivery;

import com.api.manojmobiles.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified real-time tracking payload pushed over WebSocket (/user/queue/order-tracking
 * and /topic/orders/{orderId}/tracking) whenever an agent is assigned, moves, or changes status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEventDTO {
    private TrackingEventType eventType;
    private UUID orderId;
    private String orderNumber;
    private OrderStatus status;

    /** Snapped (road-aligned) coordinates when a route polyline exists, otherwise raw GPS. */
    private Double lat;
    private Double lng;

    /** Raw GPS coordinates emitted by the device. */
    private Double rawLat;
    private Double rawLng;

    /** Compass bearing in degrees (0-360, 0 = north) for bike icon rotation. */
    private Double heading;
    private Double speedKmh;

    /** Fraction of the route completed (0.0 to 1.0). */
    private Double progress;
    private Integer remainingMetres;
    private Integer remainingSeconds;
    private LocalDateTime eta;

    /** Populated on AGENT_ASSIGNED / STATUS_CHANGE so a live client can paint the route immediately. */
    private String routePolyline;
    private Integer routeDistanceMetres;
    private Integer routeDurationSeconds;
    private DeliveryAgentResponseDTO agent;

    private String message;
    private LocalDateTime timestamp;
}
