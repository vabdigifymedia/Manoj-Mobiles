package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.review.CreateReviewRequestDTO;
import com.api.manojmobiles.dto.review.RatingSummaryDTO;
import com.api.manojmobiles.dto.review.ReviewResponseDTO;
import com.api.manojmobiles.dto.review.UpdateReviewRequestDTO;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.Review;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.OrderRepository;
import com.api.manojmobiles.repository.ProductRepository;
import com.api.manojmobiles.repository.ReviewRepository;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public ReviewResponseDTO createReview(String username, CreateReviewRequestDTO dto) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Duplicate Check
        if (reviewRepository.findByProductIdAndUserId(product.getId(), user.getId()).isPresent()) {
            throw new BadRequestException("You have already reviewed this product. Please update your existing review.");
        }

        // Verified Buyer Check
        boolean isVerifiedBuyer = orderRepository.existsByUserIdAndOrderItemsVariantProductIdAndOrderStatus(
                user.getId(), product.getId(), OrderStatus.DELIVERED);

        if (!isVerifiedBuyer) {
            throw new BadRequestException("Only verified buyers who received the product can leave a review");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(dto.getRating())
                .title(dto.getTitle())
                .comment(dto.getComment())
                .isVerifiedPurchase(true)
                .build();

        review = reviewRepository.save(review);
        recalculateProductRating(product);

        return mapToDTO(review);
    }

    @Transactional
    public ReviewResponseDTO updateReview(String username, UUID reviewId, UpdateReviewRequestDTO dto) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to update this review");
        }

        if (dto.getRating() != null) {
            review.setRating(dto.getRating());
        }
        if (dto.getTitle() != null) {
            review.setTitle(dto.getTitle());
        }
        if (dto.getComment() != null) {
            review.setComment(dto.getComment());
        }

        review = reviewRepository.save(review);
        recalculateProductRating(review.getProduct());

        return mapToDTO(review);
    }

    @Transactional
    public void deleteReview(String username, UUID reviewId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to delete this review");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);
        reviewRepository.flush(); // ensure delete is committed before recalculating
        recalculateProductRating(product);
    }

    @Transactional
    public void adminDeleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        Product product = review.getProduct();
        reviewRepository.delete(review);
        reviewRepository.flush();
        recalculateProductRating(product);
    }

    public Page<ReviewResponseDTO> getProductReviews(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable).map(this::mapToDTO);
    }

    public Page<ReviewResponseDTO> getUserReviews(String username, Pageable pageable) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return reviewRepository.findByUserId(user.getId(), pageable).map(this::mapToDTO);
    }

    public Page<ReviewResponseDTO> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(this::mapToDTO);
    }

    public RatingSummaryDTO getRatingSummary(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        long fiveStar = reviewRepository.countByProductIdAndRating(productId, 5);
        long fourStar = reviewRepository.countByProductIdAndRating(productId, 4);
        long threeStar = reviewRepository.countByProductIdAndRating(productId, 3);
        long twoStar = reviewRepository.countByProductIdAndRating(productId, 2);
        long oneStar = reviewRepository.countByProductIdAndRating(productId, 1);

        long total = fiveStar + fourStar + threeStar + twoStar + oneStar;

        return RatingSummaryDTO.builder()
                .productId(productId)
                .avgRating(product.getAvgRating() != null ? product.getAvgRating() : BigDecimal.ZERO)
                .totalReviews(product.getTotalReviews() != null ? product.getTotalReviews() : 0)
                .fiveStarCount(fiveStar)
                .fourStarCount(fourStar)
                .threeStarCount(threeStar)
                .twoStarCount(twoStar)
                .oneStarCount(oneStar)
                .fiveStarPercentage(total > 0 ? (double) fiveStar / total * 100 : 0.0)
                .fourStarPercentage(total > 0 ? (double) fourStar / total * 100 : 0.0)
                .threeStarPercentage(total > 0 ? (double) threeStar / total * 100 : 0.0)
                .twoStarPercentage(total > 0 ? (double) twoStar / total * 100 : 0.0)
                .oneStarPercentage(total > 0 ? (double) oneStar / total * 100 : 0.0)
                .build();
    }

    private void recalculateProductRating(Product product) {
        Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        long totalReviews = reviewRepository.countByProductIdAndRating(product.getId(), 1) +
                reviewRepository.countByProductIdAndRating(product.getId(), 2) +
                reviewRepository.countByProductIdAndRating(product.getId(), 3) +
                reviewRepository.countByProductIdAndRating(product.getId(), 4) +
                reviewRepository.countByProductIdAndRating(product.getId(), 5);

        if (avgRating != null) {
            product.setAvgRating(new BigDecimal(avgRating).setScale(1, RoundingMode.HALF_UP));
        } else {
            product.setAvgRating(null); // or BigDecimal.ZERO based on preference
        }
        product.setTotalReviews((int) totalReviews);
        productRepository.save(product);
    }

    private ReviewResponseDTO mapToDTO(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
