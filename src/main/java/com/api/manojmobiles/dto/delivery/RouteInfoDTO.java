package com.api.manojmobiles.dto.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteInfoDTO {
    private String encodedPolyline;
    private int distanceMetres;
    private int durationSeconds;
    private LocalDateTime eta;
    private boolean fallback;
}
