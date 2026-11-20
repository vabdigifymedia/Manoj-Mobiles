package com.api.manojmobiles.dto.address;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponseDTO {

    private UUID id;
    private String label;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private Boolean isDefault;
    private Double lat;
    private Double lng;
    private Double latitude;
    private Double longitude;

    public Double getLatitude() {
        return lat != null ? lat : latitude;
    }

    public Double getLongitude() {
        return lng != null ? lng : longitude;
    }
}
