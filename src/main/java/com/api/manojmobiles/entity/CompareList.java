package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "compare_list")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompareList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id") private ProductVariant variant;
}
