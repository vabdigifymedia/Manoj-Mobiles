package com.api.manojmobiles.service.impl;

import com.api.manojmobiles.dto.deliverypartner.CreateDeliveryPartnerRequestDTO;
import com.api.manojmobiles.dto.deliverypartner.DeliveryPartnerResponseDTO;
import com.api.manojmobiles.dto.deliverypartner.UpdateDeliveryPartnerRequestDTO;
import com.api.manojmobiles.entity.DeliveryPartner;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.DeliveryPartnerRepository;
import com.api.manojmobiles.service.DeliveryPartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryPartnerServiceImpl implements DeliveryPartnerService {

    private final DeliveryPartnerRepository deliveryPartnerRepository;

    @Override
    public DeliveryPartnerResponseDTO createDeliveryPartner(CreateDeliveryPartnerRequestDTO request) {
        DeliveryPartner partner = DeliveryPartner.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .vehicleNo(request.getVehicleNo())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        partner = deliveryPartnerRepository.save(partner);
        return mapToDTO(partner);
    }

    @Override
    public Page<DeliveryPartnerResponseDTO> getAllDeliveryPartners(String search, Pageable pageable) {
        Page<DeliveryPartner> partners;
        if (search != null && !search.trim().isEmpty()) {
            partners = deliveryPartnerRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(search, search, pageable);
        } else {
            partners = deliveryPartnerRepository.findAll(pageable);
        }
        return partners.map(this::mapToDTO);
    }

    @Override
    public DeliveryPartnerResponseDTO getDeliveryPartnerById(UUID id) {
        DeliveryPartner partner = getPartnerById(id);
        return mapToDTO(partner);
    }

    @Override
    public DeliveryPartnerResponseDTO updateDeliveryPartner(UUID id, UpdateDeliveryPartnerRequestDTO request) {
        DeliveryPartner partner = getPartnerById(id);
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            partner.setName(request.getName());
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            partner.setPhone(request.getPhone());
        }
        if (request.getVehicleNo() != null && !request.getVehicleNo().trim().isEmpty()) {
            partner.setVehicleNo(request.getVehicleNo());
        }
        
        partner = deliveryPartnerRepository.save(partner);
        return mapToDTO(partner);
    }

    @Override
    public DeliveryPartnerResponseDTO toggleActiveStatus(UUID id) {
        DeliveryPartner partner = getPartnerById(id);
        partner.setIsActive(!partner.getIsActive());
        partner = deliveryPartnerRepository.save(partner);
        return mapToDTO(partner);
    }

    @Override
    public List<DeliveryPartnerResponseDTO> getActiveDeliveryPartners() {
        return deliveryPartnerRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteDeliveryPartner(UUID id) {
        DeliveryPartner partner = getPartnerById(id);
        deliveryPartnerRepository.delete(partner);
    }

    private DeliveryPartner getPartnerById(UUID id) {
        return deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Partner not found with id: " + id));
    }

    private DeliveryPartnerResponseDTO mapToDTO(DeliveryPartner partner) {
        return DeliveryPartnerResponseDTO.builder()
                .id(partner.getId())
                .name(partner.getName())
                .phone(partner.getPhone())
                .vehicleNo(partner.getVehicleNo())
                .isActive(partner.getIsActive())
                .createdAt(partner.getCreatedAt())
                .build();
    }
}
