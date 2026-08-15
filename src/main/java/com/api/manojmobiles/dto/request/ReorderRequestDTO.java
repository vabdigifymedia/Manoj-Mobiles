package com.api.manojmobiles.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderRequestDTO {
    @NotEmpty(message = "orderedIds list cannot be empty")
    private List<UUID> orderedIds;
}
