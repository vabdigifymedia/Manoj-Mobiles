package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "product_specification")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String specGroup;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String specKey;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String specValue;
}
