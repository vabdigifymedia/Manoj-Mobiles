package com.api.manojmobiles.dto.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignShipmentRequestDTO {
    @NotNull
    private UUID agentId;
}
