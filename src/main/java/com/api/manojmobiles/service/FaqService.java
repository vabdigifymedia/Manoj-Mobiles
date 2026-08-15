package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.request.FaqRequestDTO;
import com.api.manojmobiles.dto.request.ReorderRequestDTO;
import com.api.manojmobiles.dto.response.FaqResponseDTO;

import java.util.List;
import java.util.UUID;

public interface FaqService {
    List<FaqResponseDTO> getActiveFaqs();
    List<FaqResponseDTO> getAllFaqs();
    FaqResponseDTO getFaqById(UUID id);
    FaqResponseDTO createFaq(FaqRequestDTO dto);
    FaqResponseDTO updateFaq(UUID id, FaqRequestDTO dto);
    void updateFaqStatus(UUID id, boolean isActive);
    void reorderFaqs(ReorderRequestDTO dto);
    void deleteFaq(UUID id);
}
