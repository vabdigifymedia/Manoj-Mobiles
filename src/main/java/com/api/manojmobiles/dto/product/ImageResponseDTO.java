package com.api.manojmobiles.dto.product;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResponseDTO {
    private UUID id;
    private String url;
    private Boolean isPrimary;
}
