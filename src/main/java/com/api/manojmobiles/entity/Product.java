package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String name;

    @NotBlank
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Brand brand;

    @OneToOne(fetch = FetchType.LAZY)
    private Category category;

    @OneToMany(mappedBy = "products")
    private List<ProductVariant> variants;

    @Min(0)
    private Integer warrantyMonths;

    @Min(0)
    private Integer returnPolicyDays;

    private Boolean isReturnable;

    @NotBlank
    @Column(unique = true)
    private String slug;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private BigDecimal avgRating;

    @Min(0)
    private Integer totalReviews;

    private String metaTitle;

    private String metaDescription;
    private String metaKeywords;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
