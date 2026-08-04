package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "coupon")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(unique = true)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal value;

    @DecimalMin("0.0")
    private BigDecimal minOrderAmount;

    @NotNull
    private LocalDate validFrom;

    @NotNull
    private LocalDate validTo;

    @Min(1)
    private Integer usageLimit;
}
