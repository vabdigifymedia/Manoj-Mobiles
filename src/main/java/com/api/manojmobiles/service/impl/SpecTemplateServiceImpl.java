package com.api.manojmobiles.service.impl;

import com.api.manojmobiles.dto.spectemplate.SpecGroupDTO;
import com.api.manojmobiles.dto.spectemplate.SpecTemplateRequestDTO;
import com.api.manojmobiles.dto.spectemplate.SpecTemplateResponseDTO;
import com.api.manojmobiles.entity.Category;
import com.api.manojmobiles.entity.SpecTemplate;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.CategoryRepository;
import com.api.manojmobiles.repository.SpecTemplateRepository;
import com.api.manojmobiles.service.SpecTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecTemplateServiceImpl implements SpecTemplateService {

    private final SpecTemplateRepository specTemplateRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<SpecTemplateResponseDTO> getAllSpecTemplates() {
        return specTemplateRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SpecTemplateResponseDTO getSpecTemplateByCategoryId(UUID categoryId) {
        SpecTemplate template = specTemplateRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("SpecTemplate not found for category " + categoryId));
        return mapToDTO(template);
    }

    @Override
    @Transactional
    public SpecTemplateResponseDTO saveSpecTemplate(SpecTemplateRequestDTO requestDTO) {
        Category category = categoryRepository.findById(requestDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + requestDTO.getCategoryId()));

        Optional<SpecTemplate> existingOpt = specTemplateRepository.findByCategoryId(requestDTO.getCategoryId());
        SpecTemplate template;
        
        try {
            String specJson = objectMapper.writeValueAsString(requestDTO.getGroups());
            if (existingOpt.isPresent()) {
                template = existingOpt.get();
                template.setTemplateName(requestDTO.getTemplateName());
                template.setSpecJson(specJson);
            } else {
                template = SpecTemplate.builder()
                        .category(category)
                        .templateName(requestDTO.getTemplateName())
                        .specJson(specJson)
                        .build();
            }
            SpecTemplate saved = specTemplateRepository.save(template);
            return mapToDTO(saved);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse spec groups to JSON", e);
            throw new RuntimeException("Failed to process specification groups");
        }
    }

    @Override
    @Transactional
    public void deleteSpecTemplate(UUID id) {
        if (!specTemplateRepository.existsById(id)) {
            throw new ResourceNotFoundException("SpecTemplate not found with id " + id);
        }
        specTemplateRepository.deleteById(id);
    }

    private SpecTemplateResponseDTO mapToDTO(SpecTemplate template) {
        SpecTemplateResponseDTO dto = new SpecTemplateResponseDTO();
        dto.setId(template.getId());
        dto.setCategoryId(template.getCategory().getId());
        dto.setCategoryName(template.getCategory().getName());
        dto.setTemplateName(template.getTemplateName());

        try {
            List<SpecGroupDTO> groups = objectMapper.readValue(
                    template.getSpecJson(),
                    new TypeReference<List<SpecGroupDTO>>() {}
            );
            dto.setGroups(groups);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse spec JSON to object", e);
        }

        return dto;
    }
}
