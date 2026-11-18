package com.api.manojmobiles.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveLocationResponseDTO {
    private Double lat;
    private Double lng;
    private Double rawLat;
    private Double rawLng;
    private Double heading;
    private Double speedKmh;
    private Double progress;
    private Integer remainingMetres;
    private Integer remainingSeconds;
    private LocalDateTime eta;
    private LocalDateTime recordedAt;
}
