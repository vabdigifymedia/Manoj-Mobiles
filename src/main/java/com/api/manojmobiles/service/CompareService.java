package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.personalization.CompareItemDTO;
import com.api.manojmobiles.entity.CompareList;
import com.api.manojmobiles.entity.ProductSpecification;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.CompareListRepository;
import com.api.manojmobiles.repository.ProductVariantRepository;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompareService {

    private final CompareListRepository compareListRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CompareItemDTO> getCompareList(String username) {
        User user = getUser(username);
        return compareListRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CompareItemDTO addToCompareList(String username, UUID variantId) {
        User user = getUser(username);
        ProductVariant newVariant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        List<CompareList> currentList = compareListRepository.findByUserId(user.getId());

        // Check limit
        if (currentList.size() >= 4 && currentList.stream().noneMatch(item -> item.getVariant().getId().equals(variantId))) {
            throw new BadRequestException("Compare list can contain a maximum of 4 items");
        }

        // Check category match
        if (!currentList.isEmpty()) {
            UUID existingCategoryId = currentList.get(0).getVariant().getProduct().getCategory().getId();
            if (!existingCategoryId.equals(newVariant.getProduct().getCategory().getId())) {
                throw new BadRequestException("All items in the compare list must belong to the same category");
            }
        }

        CompareList compareItem = compareListRepository.findByUserIdAndVariantId(user.getId(), variantId)
                .orElseGet(() -> {
                    CompareList newItem = CompareList.builder()
                            .user(user)
                            .variant(newVariant)
                            .build();
                    return compareListRepository.save(newItem);
                });

        return mapToDTO(compareItem);
    }

    @Transactional
    public void removeFromCompareList(String username, UUID variantId) {
        User user = getUser(username);
        compareListRepository.deleteByUserIdAndVariantId(user.getId(), variantId);
    }

    @Transactional
    public void clearCompareList(String username) {
        User user = getUser(username);
        compareListRepository.deleteByUserId(user.getId());
    }

    private User getUser(String username) {
        return userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private CompareItemDTO mapToDTO(CompareList compareList) {
        ProductVariant variant = compareList.getVariant();

        String primaryImage = null;
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            primaryImage = variant.getImages().get(0).getUrl();
        }

        Map<String, String> specsMap = new HashMap<>();
        if (variant.getSpecifications() != null) {
            for (ProductSpecification spec : variant.getSpecifications()) {
                specsMap.put(spec.getSpecKey(), spec.getSpecValue());
            }
        }

        return CompareItemDTO.builder()
                .id(compareList.getId())
                .variantId(variant.getId())
                .variantName(variant.getVariantName())
                .productName(variant.getProduct().getName())
                .price(variant.getSellingPrice())
                .imageUrl(primaryImage)
                .specifications(specsMap)
                .build();
    }
}
