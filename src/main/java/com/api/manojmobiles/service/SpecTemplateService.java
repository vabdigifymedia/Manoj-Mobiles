package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.spectemplate.SpecTemplateRequestDTO;
import com.api.manojmobiles.dto.spectemplate.SpecTemplateResponseDTO;

import java.util.List;
import java.util.UUID;

public interface SpecTemplateService {
    List<SpecTemplateResponseDTO> getAllSpecTemplates();
    SpecTemplateResponseDTO getSpecTemplateByCategoryId(UUID categoryId);
    SpecTemplateResponseDTO saveSpecTemplate(SpecTemplateRequestDTO requestDTO);
    void deleteSpecTemplate(UUID id);
}
