package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.personalization.CompareItemDTO;
import com.api.manojmobiles.dto.personalization.RecentlyViewedItemDTO;
import com.api.manojmobiles.dto.personalization.WishlistItemDTO;
import com.api.manojmobiles.service.CompareService;
import com.api.manojmobiles.service.RecentlyViewedService;
import com.api.manojmobiles.service.WishlistService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Personalization")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class PersonalizationController {

    private final WishlistService wishlistService;
    private final CompareService compareService;
    private final RecentlyViewedService recentlyViewedService;

    // --- Wishlist Endpoints ---

    @GetMapping("/wishlist")
    public ResponseEntity<ApiResponse<Page<WishlistItemDTO>>> getWishlist(
            Authentication authentication,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<WishlistItemDTO> wishlist = wishlistService.getWishlist(authentication.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched successfully", wishlist));
    }

    @PostMapping("/wishlist/{variantId}")
    public ResponseEntity<ApiResponse<WishlistItemDTO>> addToWishlist(
            Authentication authentication,
            @PathVariable UUID variantId) {
        WishlistItemDTO item = wishlistService.addToWishlist(authentication.getName(), variantId);
        return new ResponseEntity<>(ApiResponse.success("Added to wishlist", item), HttpStatus.CREATED);
    }

    @DeleteMapping("/wishlist/{variantId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            Authentication authentication,
            @PathVariable UUID variantId) {
        wishlistService.removeFromWishlist(authentication.getName(), variantId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", null));
    }

    @PostMapping("/wishlist/{variantId}/move-to-cart")
    public ResponseEntity<ApiResponse<Void>> moveToCart(
            Authentication authentication,
            @PathVariable UUID variantId) {
        wishlistService.moveToCart(authentication.getName(), variantId);
        return ResponseEntity.ok(ApiResponse.success("Moved to cart", null));
    }

    // --- Compare List Endpoints ---

    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<List<CompareItemDTO>>> getCompareList(
            Authentication authentication) {
        List<CompareItemDTO> list = compareService.getCompareList(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Compare list fetched successfully", list));
    }

    @PostMapping("/compare/{variantId}")
    public ResponseEntity<ApiResponse<CompareItemDTO>> addToCompareList(
            Authentication authentication,
            @PathVariable UUID variantId) {
        CompareItemDTO item = compareService.addToCompareList(authentication.getName(), variantId);
        return new ResponseEntity<>(ApiResponse.success("Added to compare list", item), HttpStatus.CREATED);
    }

    @DeleteMapping("/compare/{variantId}")
    public ResponseEntity<ApiResponse<Void>> removeFromCompareList(
            Authentication authentication,
            @PathVariable UUID variantId) {
        compareService.removeFromCompareList(authentication.getName(), variantId);
        return ResponseEntity.ok(ApiResponse.success("Removed from compare list", null));
    }

    @DeleteMapping("/compare")
    public ResponseEntity<ApiResponse<Void>> clearCompareList(
            Authentication authentication) {
        compareService.clearCompareList(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Compare list cleared", null));
    }

    // --- Recently Viewed Endpoints ---

    @GetMapping("/recently-viewed")
    public ResponseEntity<ApiResponse<List<RecentlyViewedItemDTO>>> getRecentlyViewed(
            Authentication authentication) {
        List<RecentlyViewedItemDTO> list = recentlyViewedService.getRecentlyViewed(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Recently viewed fetched successfully", list));
    }

    @PostMapping("/recently-viewed/{variantId}")
    public ResponseEntity<ApiResponse<Void>> recordRecentlyViewed(
            Authentication authentication,
            @PathVariable UUID variantId) {
        recentlyViewedService.recordView(authentication.getName(), variantId);
        return ResponseEntity.ok(ApiResponse.success("View recorded", null));
    }
}
