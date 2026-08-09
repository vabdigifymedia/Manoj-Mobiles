package com.api.manojmobiles.dto.city;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCityRequestDTO {
    @NotBlank(message = "City name is required")
    private String name;

    @NotBlank(message = "State name is required")
    private String state;

    @Builder.Default
    private Boolean isActive = true;
}
