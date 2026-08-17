package com.api.manojmobiles.dto.reel;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramReelResponseDTO {
    private UUID id;
    private String reelId;
    private String url;
    private Boolean isActive;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
