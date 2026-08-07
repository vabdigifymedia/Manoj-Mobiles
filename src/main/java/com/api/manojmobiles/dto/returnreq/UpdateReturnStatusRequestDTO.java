package com.api.manojmobiles.dto.returnreq;

import com.api.manojmobiles.entity.enums.ReturnStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReturnStatusRequestDTO {

    @NotNull(message = "Return status is required")
    private ReturnStatus status;

    private String adminNote;
}
