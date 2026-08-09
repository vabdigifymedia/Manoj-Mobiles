package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.personalization.RecentlyViewedItemDTO;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.RecentlyViewed;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.ProductVariantRepository;
import com.api.manojmobiles.repository.RecentlyViewedRepository;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentlyViewedService {

    private final RecentlyViewedRepository recentlyViewedRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RecentlyViewedItemDTO> getRecentlyViewed(String username) {
        User user = getUser(username);
        return recentlyViewedRepository.findByUserIdOrderByViewedAtDesc(user.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void recordView(String username, UUID variantId) {
        User user = getUser(username);
        
        Optional<RecentlyViewed> existingOpt = recentlyViewedRepository.findByUserIdAndVariantId(user.getId(), variantId);
        if (existingOpt.isPresent()) {
            RecentlyViewed existing = existingOpt.get();
            existing.setViewedAt(LocalDateTime.now());
            recentlyViewedRepository.save(existing);
        } else {
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

            RecentlyViewed newItem = RecentlyViewed.builder()
                    .user(user)
                    .variant(variant)
                    .viewedAt(LocalDateTime.now())
                    .build();
            recentlyViewedRepository.save(newItem);
        }

        // Maintain cap at 10 items
        List<RecentlyViewed> allItems = recentlyViewedRepository.findByUserIdOrderByViewedAtDesc(user.getId());
        if (allItems.size() > 10) {
            List<RecentlyViewed> toDelete = allItems.subList(10, allItems.size());
            recentlyViewedRepository.deleteAll(toDelete);
        }
    }

    private User getUser(String username) {
        return userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private RecentlyViewedItemDTO mapToDTO(RecentlyViewed item) {
        ProductVariant variant = item.getVariant();

        String primaryImage = null;
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            primaryImage = variant.getImages().get(0).getUrl();
        }

        return RecentlyViewedItemDTO.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .variantName(variant.getVariantName())
                .productName(variant.getProduct().getName())
                .price(variant.getSellingPrice())
                .imageUrl(primaryImage)
                .viewedAt(item.getViewedAt())
                .build();
    }
}
