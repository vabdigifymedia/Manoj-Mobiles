package com.api.manojmobiles.dto.pincode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PincodeCheckResponseDTO {
    private String pincode;
    private boolean isServiceable;
    private String cityName;
    private String state;
    private Integer estimatedDeliveryDays;
    private Boolean codAvailable;
}
