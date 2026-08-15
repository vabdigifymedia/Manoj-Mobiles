package com.api.manojmobiles.service.impl;

import com.api.manojmobiles.dto.request.BannerRequestDTO;
import com.api.manojmobiles.dto.request.ReorderRequestDTO;
import com.api.manojmobiles.dto.response.BannerResponseDTO;
import com.api.manojmobiles.entity.Banner;
import com.api.manojmobiles.entity.enums.BannerType;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.BannerRepository;
import com.api.manojmobiles.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    private void validateSchedule(Instant startTime, Instant endTime) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new BadRequestException("Start time must be strictly before end time");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getActiveBanners(BannerType type) {
        Instant now = Instant.now();
        return bannerRepository.findActiveBanners(type, now)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getAllBanners() {
        return bannerRepository.findAllByOrderByBannerTypeAscDisplayOrderAscCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BannerResponseDTO getBannerById(UUID id) {
        return bannerRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));
    }

    @Override
    @Transactional
    public BannerResponseDTO createBanner(BannerRequestDTO dto) {
        validateSchedule(dto.getStartTime(), dto.getEndTime());
        Banner banner = Banner.builder()
                .title(dto.getTitle())
                .subtitle(dto.getSubtitle())
                .badgeText(dto.getBadgeText())
                .imageUrl(dto.getImageUrl())
                .mobileImageUrl(dto.getMobileImageUrl())
                .linkUrl(dto.getLinkUrl())
                .ctaText(dto.getCtaText() != null ? dto.getCtaText() : "Shop Now")
                .bannerType(dto.getBannerType())
                .bgGradient(dto.getBgGradient())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .build();

        return mapToDTO(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public BannerResponseDTO updateBanner(UUID id, BannerRequestDTO dto) {
        validateSchedule(dto.getStartTime(), dto.getEndTime());
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        banner.setTitle(dto.getTitle());
        banner.setSubtitle(dto.getSubtitle());
        banner.setBadgeText(dto.getBadgeText());
        banner.setImageUrl(dto.getImageUrl());
        banner.setMobileImageUrl(dto.getMobileImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setCtaText(dto.getCtaText());
        banner.setBannerType(dto.getBannerType());
        banner.setBgGradient(dto.getBgGradient());
        banner.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : banner.getDisplayOrder());
        banner.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : banner.getIsActive());
        banner.setStartTime(dto.getStartTime());
        banner.setEndTime(dto.getEndTime());

        return mapToDTO(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public void updateBannerStatus(UUID id, boolean isActive) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));
        banner.setIsActive(isActive);
        bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public void reorderBanners(ReorderRequestDTO dto) {
        List<UUID> orderedIds = dto.getOrderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            int order = i;
            bannerRepository.findById(id).ifPresent(b -> {
                b.setDisplayOrder(order);
                bannerRepository.save(b);
            });
        }
    }

    @Override
    @Transactional
    public void deleteBanner(UUID id) {
        if (!bannerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Banner not found with ID: " + id);
        }
        bannerRepository.deleteById(id);
    }

    private BannerResponseDTO mapToDTO(Banner b) {
        BannerResponseDTO dto = new BannerResponseDTO();
        dto.setId(b.getId());
        dto.setTitle(b.getTitle());
        dto.setSubtitle(b.getSubtitle());
        dto.setBadgeText(b.getBadgeText());
        dto.setImageUrl(b.getImageUrl());
        dto.setMobileImageUrl(b.getMobileImageUrl());
        dto.setLinkUrl(b.getLinkUrl());
        dto.setCtaText(b.getCtaText());
        dto.setBannerType(b.getBannerType());
        dto.setBgGradient(b.getBgGradient());
        dto.setDisplayOrder(b.getDisplayOrder());
        dto.setIsActive(b.getIsActive());
        dto.setStartTime(b.getStartTime());
        dto.setEndTime(b.getEndTime());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setUpdatedAt(b.getUpdatedAt());
        return dto;
    }
}
