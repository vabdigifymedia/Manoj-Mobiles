package com.api.manojmobiles.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLiveLocationRequestDTO {
    @NotNull(message = "Latitude is required")
    private Double lat;

    @NotNull(message = "Longitude is required")
    private Double lng;
}
