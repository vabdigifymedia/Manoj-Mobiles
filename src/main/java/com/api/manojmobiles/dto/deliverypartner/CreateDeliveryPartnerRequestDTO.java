package com.api.manojmobiles.dto.deliverypartner;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateDeliveryPartnerRequestDTO {
    
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNo;

    @Builder.Default
    private Boolean isActive = true;
}
