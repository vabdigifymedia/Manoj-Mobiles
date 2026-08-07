package com.api.manojmobiles.dto.returnreq;

import com.api.manojmobiles.entity.enums.ReturnStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReturnRequestResponseDTO {

    private UUID id;
    private UUID orderId;
    private String orderNumber;
    private UUID orderItemId;
    private String productName;
    private String variantName;
    private String reason;
    private ReturnStatus status;
    private BigDecimal refundAmount;
    private LocalDateTime requestedAt;
    private String adminNote;
}
