package com.api.manojmobiles.dto.returnreq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateReturnRequestDTO {

    @NotNull(message = "Order Item ID is required")
    private UUID orderItemId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
