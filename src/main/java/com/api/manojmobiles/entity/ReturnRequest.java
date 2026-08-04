package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.ReturnStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "return_request")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @NotBlank
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ReturnStatus status;

    private LocalDateTime requestedAt;

    @DecimalMin("0.0")
    private BigDecimal refundAmount;
}
