package com.api.manojmobiles.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignDeliveryPartnerRequestDTO {
    @NotNull(message = "Delivery Partner ID is required")
    private UUID deliveryPartnerId;
}
