package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.cart.AddToCartRequestDTO;
import com.api.manojmobiles.dto.personalization.WishlistItemDTO;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.Wishlist;
import com.api.manojmobiles.entity.enums.ProductStatus;
import com.api.manojmobiles.entity.enums.StockStatus;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.ProductVariantRepository;
import com.api.manojmobiles.repository.UserRepository;
import com.api.manojmobiles.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    @Transactional(readOnly = true)
    public Page<WishlistItemDTO> getWishlist(String username, Pageable pageable) {
        User user = getUser(username);
        return wishlistRepository.findByUserId(user.getId(), pageable)
                .map(this::mapToDTO);
    }

    @Transactional
    public WishlistItemDTO addToWishlist(String username, UUID variantId) {
        User user = getUser(username);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        Wishlist wishlist = wishlistRepository.findByUserIdAndVariantId(user.getId(), variantId)
                .orElseGet(() -> {
                    Wishlist newItem = Wishlist.builder()
                            .user(user)
                            .variant(variant)
                            .addedAt(LocalDateTime.now())
                            .build();
                    return wishlistRepository.save(newItem);
                });

        return mapToDTO(wishlist);
    }

    @Transactional
    public void removeFromWishlist(String username, UUID variantId) {
        User user = getUser(username);
        wishlistRepository.deleteByUserIdAndVariantId(user.getId(), variantId);
    }

    @Transactional
    public void moveToCart(String username, UUID variantId) {
        User user = getUser(username);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        AddToCartRequestDTO cartRequest = new AddToCartRequestDTO();
        cartRequest.setVariantId(variantId);
        cartRequest.setQty(1);

        cartService.addToCart(username, cartRequest);

        wishlistRepository.deleteByUserIdAndVariantId(user.getId(), variantId);
    }

    private User getUser(String username) {
        return userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private WishlistItemDTO mapToDTO(Wishlist wishlist) {
        ProductVariant variant = wishlist.getVariant();
        boolean isAvailable = variant.getProduct().getStatus() == ProductStatus.ACTIVE;
        String stockStatus = "In stock";
        if (variant.getStockQty() == 0 || variant.getStockStatus() == StockStatus.OUT_OF_STOCK) {
            stockStatus = "Out of stock";
            isAvailable = false;
        } else if (variant.getStockQty() <= 5) {
            stockStatus = "Only " + variant.getStockQty() + " left";
        }

        String primaryImage = null;
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            primaryImage = variant.getImages().get(0).getUrl();
        }

        return WishlistItemDTO.builder()
                .id(wishlist.getId())
                .variantId(variant.getId())
                .variantName(variant.getVariantName())
                .productName(variant.getProduct().getName())
                .price(variant.getSellingPrice())
                .stockStatus(stockStatus)
                .isAvailable(isAvailable)
                .imageUrl(primaryImage)
                .addedAt(wishlist.getAddedAt())
                .build();
    }
}
