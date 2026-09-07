package com.api.manojmobiles.dto.city;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityResponseDTO {
    private UUID id;
    private String name;
    private String state;
    private Boolean isActive;
    private com.api.manojmobiles.entity.enums.CityCoverageRule coverageRule;
    private long totalPincodesCount;
}
