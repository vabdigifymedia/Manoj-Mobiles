package com.api.manojmobiles.dto.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityRequestDTO {
    @NotNull
    private Boolean isAvailable;
}
