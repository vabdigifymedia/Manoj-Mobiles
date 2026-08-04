package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.OfferType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "offer")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @NotNull
    @Enumerated(EnumType.STRING)
    private OfferType offerType;

    @NotBlank
    private String description;

    @NotNull
    private LocalDate validFrom;

    @NotNull
    private LocalDate validTo;
}
