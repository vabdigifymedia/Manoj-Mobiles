package com.api.manojmobiles.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentOrderDTO {
    private String orderNumber;
    private String customerName;
    private BigDecimal totalAmount;
    private String orderStatus;
    private LocalDateTime placedAt;
}
