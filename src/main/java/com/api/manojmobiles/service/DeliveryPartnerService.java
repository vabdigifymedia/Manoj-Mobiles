package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.deliverypartner.CreateDeliveryPartnerRequestDTO;
import com.api.manojmobiles.dto.deliverypartner.DeliveryPartnerResponseDTO;
import com.api.manojmobiles.dto.deliverypartner.UpdateDeliveryPartnerRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DeliveryPartnerService {
    DeliveryPartnerResponseDTO createDeliveryPartner(CreateDeliveryPartnerRequestDTO request);
    Page<DeliveryPartnerResponseDTO> getAllDeliveryPartners(String search, Pageable pageable);
    DeliveryPartnerResponseDTO getDeliveryPartnerById(UUID id);
    DeliveryPartnerResponseDTO updateDeliveryPartner(UUID id, UpdateDeliveryPartnerRequestDTO request);
    DeliveryPartnerResponseDTO toggleActiveStatus(UUID id);
    void deleteDeliveryPartner(UUID id);
    List<DeliveryPartnerResponseDTO> getActiveDeliveryPartners();
}
