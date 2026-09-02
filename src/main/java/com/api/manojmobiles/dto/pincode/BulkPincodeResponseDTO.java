package com.api.manojmobiles.dto.pincode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPincodeResponseDTO {

    private int totalProcessed;
    private int addedCount;
    private int skippedCount;
    private int failedCount;

    @Builder.Default
    private List<String> addedPincodes = new ArrayList<>();

    @Builder.Default
    private List<String> skippedPincodes = new ArrayList<>();

    @Builder.Default
    private List<String> failureReasons = new ArrayList<>();
}
