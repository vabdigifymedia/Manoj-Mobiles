package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "serviceable_pincode")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceablePincode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    @Column(unique = true)
    private String pincode;

    @NonNull
    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    @Min(1)
    private Integer estimatedDeliveryDays;

    private Boolean codAvailable;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
