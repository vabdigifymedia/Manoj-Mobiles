package com.api.manojmobiles.dto.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LocationUpdateRequestDTO {
    @NotNull
    private UUID orderId;
    @NotNull
    private Double lat;
    @NotNull
    private Double lng;
}
