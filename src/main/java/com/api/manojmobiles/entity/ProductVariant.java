package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.StockStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotBlank
    @Column(columnDefinition = "text", length = 10485760)
    private String variantName;

    @NotBlank
    @Column(unique = true, length = 10485760)
    private String sku;

    @Column(columnDefinition = "text", length = 10485760)
    private String color;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal mrp;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal sellingPrice;

    @Min(0) @Max(100)
    private Integer discountPercent;

    @DecimalMin("0.0")
    private BigDecimal gstPercent;

    @NotNull
    @Min(0)
    private Integer stockQty;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus;

    private Boolean codAvailable;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpecification> specifications;
}
