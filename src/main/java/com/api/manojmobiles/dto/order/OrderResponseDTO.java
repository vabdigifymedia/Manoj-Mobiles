package com.api.manojmobiles.dto.order;

import com.api.manojmobiles.entity.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    private UUID id;
    private String orderNumber;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryCharge;
    private OrderStatus orderStatus;
    private LocalDateTime placedAt;
    private List<OrderItemResponseDTO> orderItems;
}