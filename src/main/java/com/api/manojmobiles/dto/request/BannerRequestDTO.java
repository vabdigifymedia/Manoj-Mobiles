package com.api.manojmobiles.dto.request;

import com.api.manojmobiles.entity.enums.BannerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class BannerRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;
    private String badgeText;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    private String mobileImageUrl;

    @NotBlank(message = "Link URL is required")
    private String linkUrl;

    private String ctaText = "Shop Now";

    @NotNull(message = "Banner type is required")
    private BannerType bannerType;

    private String bgGradient;
    private Integer displayOrder = 0;
    private Boolean isActive = true;

    private Instant startTime;
    private Instant endTime;
}
