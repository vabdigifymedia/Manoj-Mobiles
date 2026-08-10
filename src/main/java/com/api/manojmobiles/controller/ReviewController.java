package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.review.CreateReviewRequestDTO;
import com.api.manojmobiles.dto.review.RatingSummaryDTO;
import com.api.manojmobiles.dto.review.ReviewResponseDTO;
import com.api.manojmobiles.dto.review.UpdateReviewRequestDTO;
import com.api.manojmobiles.service.ReviewService;
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
public class ReviewController {

    private final ReviewService reviewService;

    // --- Customer Endpoints ---

    @PostMapping("/api/user/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> createReview(
            Principal principal,
            @Valid @RequestBody CreateReviewRequestDTO dto) {
        ReviewResponseDTO review = reviewService.createReview(principal.getName(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", review));
    }

    @PutMapping("/api/user/reviews/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> updateReview(
            Principal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequestDTO dto) {
        ReviewResponseDTO review = reviewService.updateReview(principal.getName(), id, dto);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", review));
    }

    @DeleteMapping("/api/user/reviews/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            Principal principal,
            @PathVariable UUID id) {
        reviewService.deleteReview(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    @GetMapping("/api/user/reviews/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDTO>>> getMyReviews(
            Principal principal,
            Pageable pageable) {
        Page<ReviewResponseDTO> reviews = reviewService.getUserReviews(principal.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Your reviews fetched", reviews));
    }

    // --- Public Endpoints ---

    @GetMapping("/api/public/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDTO>>> getProductReviews(
            @PathVariable UUID productId,
            Pageable pageable) {
        Page<ReviewResponseDTO> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Product reviews fetched", reviews));
    }

    @GetMapping("/api/public/products/{productId}/ratings-summary")
    public ResponseEntity<ApiResponse<RatingSummaryDTO>> getProductRatingSummary(
            @PathVariable UUID productId) {
        RatingSummaryDTO summary = reviewService.getRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success("Product rating summary fetched", summary));
    }

    // --- Admin Endpoints ---

    @GetMapping("/api/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDTO>>> getAllReviews(Pageable pageable) {
        Page<ReviewResponseDTO> reviews = reviewService.getAllReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success("All reviews fetched", reviews));
    }

    @DeleteMapping("/api/admin/reviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteReview(@PathVariable UUID id) {
        reviewService.adminDeleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted by admin", null));
    }
}
