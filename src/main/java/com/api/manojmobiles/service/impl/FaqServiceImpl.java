package com.api.manojmobiles.service.impl;

import com.api.manojmobiles.dto.request.FaqRequestDTO;
import com.api.manojmobiles.dto.request.ReorderRequestDTO;
import com.api.manojmobiles.dto.response.FaqResponseDTO;
import com.api.manojmobiles.entity.Faq;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.FaqRepository;
import com.api.manojmobiles.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponseDTO> getActiveFaqs() {
        return faqRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponseDTO> getAllFaqs() {
        return faqRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FaqResponseDTO getFaqById(UUID id) {
        return faqRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found with ID: " + id));
    }

    @Override
    @Transactional
    public FaqResponseDTO createFaq(FaqRequestDTO dto) {
        Faq faq = Faq.builder()
                .question(dto.getQuestion())
                .answer(dto.getAnswer())
                .category(dto.getCategory())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return mapToDTO(faqRepository.save(faq));
    }

    @Override
    @Transactional
    public FaqResponseDTO updateFaq(UUID id, FaqRequestDTO dto) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found with ID: " + id));

        faq.setQuestion(dto.getQuestion());
        faq.setAnswer(dto.getAnswer());
        faq.setCategory(dto.getCategory());
        faq.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : faq.getDisplayOrder());
        faq.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : faq.getIsActive());

        return mapToDTO(faqRepository.save(faq));
    }

    @Override
    @Transactional
    public void updateFaqStatus(UUID id, boolean isActive) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found with ID: " + id));
        faq.setIsActive(isActive);
        faqRepository.save(faq);
    }

    @Override
    @Transactional
    public void reorderFaqs(ReorderRequestDTO dto) {
        List<UUID> orderedIds = dto.getOrderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            int order = i;
            faqRepository.findById(id).ifPresent(f -> {
                f.setDisplayOrder(order);
                faqRepository.save(f);
            });
        }
    }

    @Override
    @Transactional
    public void deleteFaq(UUID id) {
        if (!faqRepository.existsById(id)) {
            throw new ResourceNotFoundException("FAQ not found with ID: " + id);
        }
        faqRepository.deleteById(id);
    }

    private FaqResponseDTO mapToDTO(Faq f) {
        FaqResponseDTO dto = new FaqResponseDTO();
        dto.setId(f.getId());
        dto.setQuestion(f.getQuestion());
        dto.setAnswer(f.getAnswer());
        dto.setCategory(f.getCategory());
        dto.setDisplayOrder(f.getDisplayOrder());
        dto.setIsActive(f.getIsActive());
        dto.setCreatedAt(f.getCreatedAt());
        dto.setUpdatedAt(f.getUpdatedAt());
        return dto;
    }
}
