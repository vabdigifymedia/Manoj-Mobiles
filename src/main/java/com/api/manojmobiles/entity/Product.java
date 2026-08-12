package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(columnDefinition = "text", length = 10485760)
    private String name;

    @Column(columnDefinition = "text", length = 10485760)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductHighlight> highlights = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @Min(0)
    private Integer warrantyMonths;

    @Min(0)
    private Integer returnPolicyDays;

    private Boolean isReturnable;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text", length = 10485760)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(columnDefinition = "text", length = 10485760, unique = true)
    private String slug;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private BigDecimal avgRating;

    @Min(0)
    private Integer totalReviews;

    @Column(columnDefinition = "text", length = 10485760)
    private String metaTitle;

    @Column(columnDefinition = "text", length = 10485760)
    private String metaDescription;

    @Column(columnDefinition = "text", length = 10485760)
    private String metaKeywords;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    public void autoGenerateSlug() {
        if (this.name != null && (this.slug == null || this.slug.isBlank())) {
            String base = this.name.toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            if (base.isBlank()) base = "product";
            if (base.length() > 200) base = base.substring(0, 200);
            this.slug = base;
        }
    }
}
