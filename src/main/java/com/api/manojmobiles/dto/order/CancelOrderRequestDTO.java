package com.api.manojmobiles.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelOrderRequestDTO {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
