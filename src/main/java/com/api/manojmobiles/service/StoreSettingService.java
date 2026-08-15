package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.request.StoreSettingRequestDTO;
import com.api.manojmobiles.dto.response.StoreSettingResponseDTO;

public interface StoreSettingService {
    StoreSettingResponseDTO getStoreSettings();
    StoreSettingResponseDTO updateStoreSettings(StoreSettingRequestDTO dto);
}
