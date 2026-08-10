package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.qna.AnswerResponseDTO;
import com.api.manojmobiles.dto.qna.CreateAnswerRequestDTO;
import com.api.manojmobiles.dto.qna.CreateQuestionRequestDTO;
import com.api.manojmobiles.dto.qna.QuestionResponseDTO;
import com.api.manojmobiles.service.ProductQnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProductQnaController {

    private final ProductQnaService qnaService;

    // --- Customer Endpoints ---

    @PostMapping("/api/user/questions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<QuestionResponseDTO>> askQuestion(
            Principal principal,
            @Valid @RequestBody CreateQuestionRequestDTO dto) {
        QuestionResponseDTO question = qnaService.askQuestion(principal.getName(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question submitted successfully", question));
    }

    @PostMapping("/api/user/questions/{questionId}/answers")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AnswerResponseDTO>> addCommunityAnswer(
            Principal principal,
            @PathVariable UUID questionId,
            @Valid @RequestBody CreateAnswerRequestDTO dto) {
        // Customer answer will have isSellerAnswer = false automatically in Service layer (based on user role check)
        AnswerResponseDTO answer = qnaService.answerQuestion(principal.getName(), questionId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Community answer added successfully", answer));
    }

    // --- Public Endpoints ---

    @GetMapping("/api/public/products/{productId}/questions")
    public ResponseEntity<ApiResponse<Page<QuestionResponseDTO>>> getProductQuestions(
            @PathVariable UUID productId,
            Pageable pageable) {
        Page<QuestionResponseDTO> questions = qnaService.getApprovedQuestionsForProduct(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Product questions fetched", questions));
    }

    // --- Admin Endpoints ---

    @PostMapping("/api/admin/questions/{questionId}/answers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnswerResponseDTO>> addSellerAnswer(
            Principal principal,
            @PathVariable UUID questionId,
            @Valid @RequestBody CreateAnswerRequestDTO dto) {
        AnswerResponseDTO answer = qnaService.answerQuestion(principal.getName(), questionId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seller answer added successfully", answer));
    }

    @DeleteMapping("/api/admin/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable UUID questionId) {
        qnaService.adminDeleteQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.success("Question deleted successfully", null));
    }
}
