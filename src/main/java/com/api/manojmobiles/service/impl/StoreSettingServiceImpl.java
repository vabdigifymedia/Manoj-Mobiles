package com.api.manojmobiles.service.impl;

import com.api.manojmobiles.dto.request.StoreSettingRequestDTO;
import com.api.manojmobiles.dto.response.StoreSettingResponseDTO;
import com.api.manojmobiles.entity.StoreSetting;
import com.api.manojmobiles.repository.StoreSettingRepository;
import com.api.manojmobiles.service.StoreSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreSettingServiceImpl implements StoreSettingService {

    private final StoreSettingRepository storeSettingRepository;
    private static final String DEFAULT_CONFIG_ID = "DEFAULT_CONFIG";

    @Override
    @Transactional(readOnly = true)
    public StoreSettingResponseDTO getStoreSettings() {
        return mapToDTO(getCurrentSettings());
    }

    @Override
    @Transactional(readOnly = true)
    public StoreSetting getCurrentSettings() {
        return storeSettingRepository.findById(DEFAULT_CONFIG_ID)
                .orElseGet(StoreSetting::new);
    }

    @Override
    @Transactional
    public StoreSettingResponseDTO updateStoreSettings(StoreSettingRequestDTO dto) {
        StoreSetting setting = storeSettingRepository.findById(DEFAULT_CONFIG_ID)
                .orElse(new StoreSetting());

        setting.setStoreName(dto.getStoreName());
        setting.setAnnouncementText(dto.getAnnouncementText());
        setting.setAnnouncementLink(dto.getAnnouncementLink());
        setting.setAnnouncementActive(dto.getAnnouncementActive() != null ? dto.getAnnouncementActive() : setting.getAnnouncementActive());
        setting.setWhatsappNumber(dto.getWhatsappNumber());
        setting.setWhatsappDefaultMessage(dto.getWhatsappDefaultMessage());
        setting.setSupportPhone(dto.getSupportPhone());
        setting.setSupportEmail(dto.getSupportEmail());
        setting.setStoreAddress(dto.getStoreAddress());
        setting.setStoreTimings(dto.getStoreTimings());
        setting.setGoogleMapsUrl(dto.getGoogleMapsUrl());
        if (dto.getFreeDeliveryThreshold() != null) {
            setting.setFreeDeliveryThreshold(dto.getFreeDeliveryThreshold());
        }
        if (dto.getExpressDeliveryText() != null) {
            setting.setExpressDeliveryText(dto.getExpressDeliveryText());
        }
        if (dto.getHyperlocalEnabled() != null) {
            setting.setHyperlocalEnabled(dto.getHyperlocalEnabled());
        }
        if (dto.getHyperlocalDeliveryCharge() != null) {
            setting.setHyperlocalDeliveryCharge(dto.getHyperlocalDeliveryCharge());
        }
        if (dto.getHyperlocalEstimatedMins() != null) {
            setting.setHyperlocalEstimatedMins(dto.getHyperlocalEstimatedMins());
        }
        if (dto.getHyperlocalMaxRadiusKm() != null) {
            setting.setHyperlocalMaxRadiusKm(dto.getHyperlocalMaxRadiusKm());
        }
        if (dto.getStandardDeliveryCharge() != null) {
            setting.setStandardDeliveryCharge(dto.getStandardDeliveryCharge());
        }
        if (dto.getStandardEstimatedDays() != null) {
            setting.setStandardEstimatedDays(dto.getStandardEstimatedDays());
        }
        setting.setStoreLat(dto.getStoreLat());
        setting.setStoreLng(dto.getStoreLng());

        return mapToDTO(storeSettingRepository.save(setting));
    }

    private StoreSettingResponseDTO mapToDTO(StoreSetting s) {
        StoreSettingResponseDTO dto = new StoreSettingResponseDTO();
        dto.setId(s.getId());
        dto.setStoreName(s.getStoreName());
        dto.setAnnouncementText(s.getAnnouncementText());
        dto.setAnnouncementLink(s.getAnnouncementLink());
        dto.setAnnouncementActive(s.getAnnouncementActive());
        dto.setWhatsappNumber(s.getWhatsappNumber());
        dto.setWhatsappDefaultMessage(s.getWhatsappDefaultMessage());
        dto.setSupportPhone(s.getSupportPhone());
        dto.setSupportEmail(s.getSupportEmail());
        dto.setStoreAddress(s.getStoreAddress());
        dto.setStoreTimings(s.getStoreTimings());
        dto.setGoogleMapsUrl(s.getGoogleMapsUrl());
        dto.setFreeDeliveryThreshold(s.getFreeDeliveryThreshold());
        dto.setExpressDeliveryText(s.getExpressDeliveryText());
        dto.setHyperlocalEnabled(s.getHyperlocalEnabled() != null ? s.getHyperlocalEnabled() : true);
        dto.setHyperlocalDeliveryCharge(s.getHyperlocalDeliveryCharge() != null ? s.getHyperlocalDeliveryCharge() : java.math.BigDecimal.valueOf(49));
        dto.setHyperlocalEstimatedMins(s.getHyperlocalEstimatedMins() != null ? s.getHyperlocalEstimatedMins() : 45);
        dto.setHyperlocalMaxRadiusKm(s.getHyperlocalMaxRadiusKm() != null ? s.getHyperlocalMaxRadiusKm() : 15.0);
        dto.setStandardDeliveryCharge(s.getStandardDeliveryCharge() != null ? s.getStandardDeliveryCharge() : java.math.BigDecimal.ZERO);
        dto.setStandardEstimatedDays(s.getStandardEstimatedDays() != null ? s.getStandardEstimatedDays() : 4);
        dto.setStoreLat(s.getStoreLat());
        dto.setStoreLng(s.getStoreLng());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }
}
