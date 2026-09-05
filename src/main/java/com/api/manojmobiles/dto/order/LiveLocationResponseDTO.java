package com.api.manojmobiles.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveLocationResponseDTO {
    private Double lat;
    private Double lng;
}
