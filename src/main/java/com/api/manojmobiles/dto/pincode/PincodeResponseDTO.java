package com.api.manojmobiles.dto.pincode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PincodeResponseDTO {
    private UUID id;
    private String pincode;
    private UUID cityId;
    private String cityName;
    private String state;
    private Integer estimatedDeliveryDays;
    private Boolean codAvailable;
}
