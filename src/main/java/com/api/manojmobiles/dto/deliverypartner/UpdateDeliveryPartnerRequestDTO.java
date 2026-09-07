package com.api.manojmobiles.dto.deliverypartner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDeliveryPartnerRequestDTO {
    private String name;
    private String phone;
    private String vehicleNo;
}
