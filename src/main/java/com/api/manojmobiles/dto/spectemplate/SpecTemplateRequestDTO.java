package com.api.manojmobiles.dto.spectemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SpecTemplateRequestDTO {
    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Template name is required")
    private String templateName;

    private List<SpecGroupDTO> groups;
}
