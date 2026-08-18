package com.api.manojmobiles.dto.spectemplate;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class SpecTemplateResponseDTO {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String templateName;
    private List<SpecGroupDTO> groups;
}
