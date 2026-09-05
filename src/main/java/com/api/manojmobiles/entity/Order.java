package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    @Column(unique = true)
    private String orderNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal totalAmount;

    @DecimalMin("0.0")
    private BigDecimal discountAmount;

    @DecimalMin("0.0")
    private BigDecimal deliveryCharge;

    @DecimalMin("0.0")
    private BigDecimal gstAmount;

    private String invoiceNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private LocalDateTime placedAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryType;

    private String trackingId;
    private String courierPartner;

    private LocalDateTime expectedDeliveryDate;

    private Double shippingLat;
    private Double shippingLng;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_partner_id")
    private DeliveryPartner deliveryPartner;

    @OneToMany(mappedBy = "order") private List<OrderItem> orderItems;
    @OneToMany(mappedBy = "order") private List<OrderStatusHistory> statusHistory;
    @OneToOne(mappedBy = "order") private Payment payment;
    @OneToOne(mappedBy = "order") private Shipment shipment;
}
