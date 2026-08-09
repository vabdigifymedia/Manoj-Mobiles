package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wishlist")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id") private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id") private ProductVariant variant;

    private LocalDateTime addedAt;
}
