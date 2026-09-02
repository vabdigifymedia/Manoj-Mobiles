package com.api.manojmobiles.dto.enquiry;

import com.api.manojmobiles.entity.enums.EnquiryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEnquiryStatusRequestDTO {
    
    @NotNull(message = "Status is required")
    private EnquiryStatus status;
}
