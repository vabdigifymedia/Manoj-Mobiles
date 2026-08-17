package com.api.manojmobiles.dto.reel;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramReelRequestDTO {
    
    @NotBlank(message = "Reel ID or URL is required")
    private String reelId; // Admin can paste full URL, we will extract ID in service

    private Boolean isActive;
    
    private Integer displayOrder;
}
