package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupon_usage", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"coupon_id", "user_id"}, name = "uk_coupon_user")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private LocalDateTime usedAt;
}
