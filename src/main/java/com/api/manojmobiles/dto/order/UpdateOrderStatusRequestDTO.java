package com.api.manojmobiles.dto.order;

import com.api.manojmobiles.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequestDTO {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    private String note;
}
