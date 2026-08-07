package com.api.manojmobiles.dto.product;

import com.api.manojmobiles.entity.enums.InventoryReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustmentRequestDTO {
    
    @NotNull(message = "Change quantity is required")
    private Integer changeQty;

    @NotNull(message = "Inventory reason is required")
    private InventoryReason reason;
}
