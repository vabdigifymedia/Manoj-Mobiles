package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.BannerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(length = 100)
    private String badgeText;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(length = 1000)
    private String mobileImageUrl;

    @Column(nullable = false, length = 500)
    private String linkUrl;

    @Column(length = 50)
    private String ctaText = "Shop Now";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BannerType bannerType;

    @Column(length = 100)
    private String bgGradient;

    private Integer displayOrder = 0;

    private Boolean isActive = true;

    private Instant startTime;
    private Instant endTime;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
