package com.api.manojmobiles.dto.delivery;

import com.api.manojmobiles.entity.enums.DeliveryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOptionsResponseDTO {
    private String pincode;
    private boolean hyperlocalAvailable;
    private boolean standardAvailable;
    private Double distanceFromStoreKm;
    private DeliveryType recommendedType;
    private List<DeliveryOptionDTO> options;
}
