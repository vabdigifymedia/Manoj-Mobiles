package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "delivery_partners")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    @NotBlank
    private String vehicleNo;

    @Builder.Default
    private Boolean isActive = true;

    @org.hibernate.annotations.CreationTimestamp
    private java.time.LocalDateTime createdAt;
}
