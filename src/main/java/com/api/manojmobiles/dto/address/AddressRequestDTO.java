package com.api.manojmobiles.dto.address;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequestDTO {

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

    @JsonAlias({"latitude"})
    private Double lat;

    @JsonAlias({"longitude"})
    private Double lng;

    private Double latitude;
    private Double longitude;

    private Boolean isDefault;

    // Helper to accept either lat or latitude
    public Double getEffectiveLat() {
        return lat != null ? lat : latitude;
    }

    public Double getEffectiveLng() {
        return lng != null ? lng : longitude;
    }
}
