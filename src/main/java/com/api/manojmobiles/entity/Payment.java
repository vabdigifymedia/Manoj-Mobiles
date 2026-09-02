package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String txnId;

    private String pgTransactionId; // Pine Labs order/transaction ID

    private String paymentMode; // UPI, CREDIT_CARD, DEBIT_CARD etc.

    private LocalDateTime paidAt;
}
