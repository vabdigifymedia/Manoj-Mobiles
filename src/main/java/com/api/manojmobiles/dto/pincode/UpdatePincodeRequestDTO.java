package com.api.manojmobiles.dto.pincode;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePincodeRequestDTO {
    @Min(value = 1, message = "Estimated delivery days must be at least 1")
    private Integer estimatedDeliveryDays;

    private Boolean codAvailable;
}
