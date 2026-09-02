package com.api.manojmobiles.dto.pincode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePincodeRequestDTO {
    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    @NotNull(message = "City ID is required")
    private UUID cityId;

    @Min(value = 1, message = "Estimated delivery days must be at least 1")
    private Integer estimatedDeliveryDays;

    @Builder.Default
    private Boolean codAvailable = true;

    @Builder.Default
    private Boolean isActive = true;
}
