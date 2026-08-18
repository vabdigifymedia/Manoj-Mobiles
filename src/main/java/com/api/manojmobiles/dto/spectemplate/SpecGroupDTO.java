package com.api.manojmobiles.dto.spectemplate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SpecGroupDTO {
    @NotBlank(message = "Group name is required")
    private String groupName;

    private List<String> specKeys;
}
