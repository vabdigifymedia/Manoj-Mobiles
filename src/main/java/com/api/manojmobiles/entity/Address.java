package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "address")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    private String label;

    @NotBlank
    private String addressLine;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    private String pincode;

    @Column(name = "latitude")
    private Double lat;

    @Column(name = "longitude")
    private Double lng;

    private Boolean isDefault;

    public Double getLatitude() {
        return lat;
    }

    public void setLatitude(Double latitude) {
        this.lat = latitude;
    }

    public Double getLongitude() {
        return lng;
    }

    public void setLongitude(Double longitude) {
        this.lng = longitude;
    }

    public Double getEffectiveLat() {
        return lat;
    }

    public Double getEffectiveLng() {
        return lng;
    }
}
