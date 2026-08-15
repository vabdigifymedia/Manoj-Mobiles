package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.request.BannerRequestDTO;
import com.api.manojmobiles.dto.request.ReorderRequestDTO;
import com.api.manojmobiles.dto.response.BannerResponseDTO;
import com.api.manojmobiles.entity.enums.BannerType;

import java.util.List;
import java.util.UUID;

public interface BannerService {
    List<BannerResponseDTO> getActiveBanners(BannerType type);
    List<BannerResponseDTO> getAllBanners();
    BannerResponseDTO getBannerById(UUID id);
    BannerResponseDTO createBanner(BannerRequestDTO dto);
    BannerResponseDTO updateBanner(UUID id, BannerRequestDTO dto);
    void updateBannerStatus(UUID id, boolean isActive);
    void reorderBanners(ReorderRequestDTO dto);
    void deleteBanner(UUID id);
}
