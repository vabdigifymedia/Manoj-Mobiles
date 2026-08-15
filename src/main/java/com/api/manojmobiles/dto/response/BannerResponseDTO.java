package com.api.manojmobiles.dto.response;

import com.api.manojmobiles.entity.enums.BannerType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class BannerResponseDTO {
    private UUID id;
    private String title;
    private String subtitle;
    private String badgeText;
    private String imageUrl;
    private String mobileImageUrl;
    private String linkUrl;
    private String ctaText;
    private BannerType bannerType;
    private String bgGradient;
    private Integer displayOrder;
    private Boolean isActive;
    private Instant startTime;
    private Instant endTime;
    private Instant createdAt;
    private Instant updatedAt;
}
