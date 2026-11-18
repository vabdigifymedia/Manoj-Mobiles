package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private DeliveryAgent agent;

    private LocalDateTime assignedAt;
    private LocalDateTime pickedAt;
    private LocalDateTime deliveredAt;

    @NotNull
    @Enumerated(EnumType.STRING) private OrderStatus currentStatus;

    @Column(columnDefinition = "TEXT")
    private String routePolyline;

    private Integer routeDistanceMetres;
    private Integer routeDurationSeconds;

    private Double storeLat;
    private Double storeLng;

    private Double progress;
    private Integer remainingMetres;
    private Integer remainingSeconds;
    private LocalDateTime eta;

    private Double lastLat;
    private Double lastLng;
    private Double lastHeading;
    private LocalDateTime lastLocationAt;
}
