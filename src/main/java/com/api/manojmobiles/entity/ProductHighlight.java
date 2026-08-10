package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.AllowedIcon;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "product_highlights")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AllowedIcon iconName;

    @NotBlank
    private String text;

    @NotNull
    private Integer displayOrder;
}
