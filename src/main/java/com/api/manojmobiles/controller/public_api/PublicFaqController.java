package com.api.manojmobiles.controller.public_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.response.FaqResponseDTO;
import com.api.manojmobiles.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/faqs")
@RequiredArgsConstructor
@Tag(name = "Public FAQ", description = "Endpoints for fetching active FAQs")
public class PublicFaqController {

    private final FaqService faqService;

    @GetMapping
    @Operation(summary = "Get active FAQs", description = "Fetches all active FAQs")
    public ResponseEntity<ApiResponse<List<FaqResponseDTO>>> getActiveFaqs() {
        List<FaqResponseDTO> faqs = faqService.getActiveFaqs();
        return ResponseEntity.ok(ApiResponse.success("FAQs retrieved successfully", faqs));
    }
}
