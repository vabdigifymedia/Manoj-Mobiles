package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.request.StoreSettingRequestDTO;
import com.api.manojmobiles.dto.response.StoreSettingResponseDTO;
import com.api.manojmobiles.entity.StoreSetting;

public interface StoreSettingService {
    StoreSettingResponseDTO getStoreSettings();
    StoreSettingResponseDTO updateStoreSettings(StoreSettingRequestDTO dto);
    StoreSetting getCurrentSettings();
}
