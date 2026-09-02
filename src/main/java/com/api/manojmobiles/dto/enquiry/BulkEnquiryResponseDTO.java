package com.api.manojmobiles.dto.enquiry;

import com.api.manojmobiles.entity.enums.EnquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnquiryResponseDTO {
    private UUID id;
    
    private UUID productId;
    private String productName;
    
    private UUID variantId;
    private String variantName; // E.g., "Olive Green"
    
    private String name;
    private String email;
    private String mobileNumber;
    private String companyName;
    private String gstin;
    private Integer estimatedQuantity;
    private String requirements;
    
    private EnquiryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
