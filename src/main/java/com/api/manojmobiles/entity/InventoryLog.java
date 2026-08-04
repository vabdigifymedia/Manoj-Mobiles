package com.api.manojmobiles.entity;

import com.api.manojmobiles.entity.enums.InventoryReason;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @NotNull
    private Integer changeQty;

    @NotNull
    @Enumerated(EnumType.STRING)
    private InventoryReason reason;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
