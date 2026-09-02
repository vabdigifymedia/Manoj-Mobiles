package com.api.manojmobiles.dto.pincode;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPincodeUploadRequestDTO {

    @NotNull(message = "City ID is required")
    private UUID cityId;

    /**
     * Simple list of 6-digit pincode strings (e.g., ["121001", "121002"]).
     */
    private List<String> pincodes;

    /**
     * Optional granular items if individual settings (delivery days, cod) are desired.
     */
    private List<CreatePincodeRequestDTO> items;

    /**
     * Default estimated delivery days for pincodes uploaded in this batch.
     */
    @Builder.Default
    private Integer estimatedDeliveryDays = 3;

    /**
     * Default COD availability for pincodes uploaded in this batch.
     */
    @Builder.Default
    private Boolean codAvailable = true;

    /**
     * Default active status for pincodes uploaded in this batch.
     */
    @Builder.Default
    private Boolean isActive = true;
}
