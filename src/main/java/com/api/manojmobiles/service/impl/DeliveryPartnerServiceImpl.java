package com.api.manojmobiles.service.impl;

import com.api.manojmobiles.dto.deliverypartner.CreateDeliveryPartnerRequestDTO;
import com.api.manojmobiles.dto.deliverypartner.DeliveryPartnerResponseDTO;
import com.api.manojmobiles.dto.deliverypartner.UpdateDeliveryPartnerRequestDTO;
import com.api.manojmobiles.entity.DeliveryAgent;
import com.api.manojmobiles.entity.DeliveryPartner;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.Role;
import com.api.manojmobiles.entity.enums.UserStatus;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.DeliveryAgentRepository;
import com.api.manojmobiles.repository.DeliveryPartnerRepository;
import com.api.manojmobiles.repository.UserRepository;
import com.api.manojmobiles.service.DeliveryPartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryPartnerServiceImpl implements DeliveryPartnerService {

    private static final String DEFAULT_PASSWORD = "Password@123";

    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public DeliveryPartnerResponseDTO createDeliveryPartner(CreateDeliveryPartnerRequestDTO request) {
        String cleanPhone = request.getPhone().trim();
        String cleanEmail = (request.getEmail() != null && !request.getEmail().trim().isEmpty())
                ? request.getEmail().trim()
                : cleanPhone + "@rider.emistore.in";
        String rawPassword = (request.getPassword() != null && !request.getPassword().trim().isEmpty())
                ? request.getPassword().trim()
                : DEFAULT_PASSWORD;

        // 1. Find or create the underlying login User
        User user = userRepository.findByEmail(cleanEmail)
                .or(() -> userRepository.findByPhone(cleanPhone))
                .orElseGet(() -> User.builder()
                        .name(request.getName().trim())
                        .email(cleanEmail)
                        .phone(cleanPhone)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .role(Role.DELIVERY_AGENT)
                        .status(UserStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build());

        // Ensure user is marked as a delivery agent and active
        user.setRole(Role.DELIVERY_AGENT);
        user.setStatus(UserStatus.ACTIVE);
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        user = userRepository.save(user);

        // 2. Find or create the corresponding DeliveryAgent record
        final User finalUser = user;
        DeliveryAgent agent = deliveryAgentRepository.findByUserId(user.getId())
                .orElseGet(() -> DeliveryAgent.builder()
                        .user(finalUser)
                        .vehicleNo(request.getVehicleNo().trim())
                        .isAvailable(request.getIsActive() != null ? request.getIsActive() : true)
                        .build());
        agent.setVehicleNo(request.getVehicleNo().trim());
        if (request.getIsActive() != null) {
            agent.setIsAvailable(request.getIsActive());
        }
        agent = deliveryAgentRepository.save(agent);

        // 3. Save the DeliveryPartner record linked to this User & Agent
        DeliveryPartner partner = deliveryPartnerRepository.findByPhone(cleanPhone)
                .orElseGet(() -> DeliveryPartner.builder().build());

        partner.setName(request.getName().trim());
        partner.setPhone(cleanPhone);
        partner.setEmail(cleanEmail);
        partner.setVehicleNo(request.getVehicleNo().trim());
        partner.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        partner.setUser(user);
        partner.setAgent(agent);
        partner = deliveryPartnerRepository.save(partner);

        log.info("Unified Delivery Partner & Agent created: {} ({}) | Login: {} / {}",
                partner.getName(), partner.getVehicleNo(), cleanEmail, rawPassword);

        return mapToDTO(partner);
    }

    @Override
    @Transactional
    public Page<DeliveryPartnerResponseDTO> getAllDeliveryPartners(String search, Pageable pageable) {
        Page<DeliveryPartner> partners;
        if (search != null && !search.trim().isEmpty()) {
            partners = deliveryPartnerRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(search, search, pageable);
        } else {
            partners = deliveryPartnerRepository.findAll(pageable);
        }
        return partners.map(p -> mapToDTO(ensureLinkedAgent(p)));
    }

    @Override
    @Transactional
    public DeliveryPartnerResponseDTO getDeliveryPartnerById(UUID id) {
        DeliveryPartner partner = getPartnerById(id);
        partner = ensureLinkedAgent(partner);
        return mapToDTO(partner);
    }

    @Override
    @Transactional
    public DeliveryPartnerResponseDTO updateDeliveryPartner(UUID id, UpdateDeliveryPartnerRequestDTO request) {
        DeliveryPartner partner = getPartnerById(id);
        partner = ensureLinkedAgent(partner);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            partner.setName(request.getName().trim());
            if (partner.getUser() != null) {
                partner.getUser().setName(request.getName().trim());
            }
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            partner.setPhone(request.getPhone().trim());
            if (partner.getUser() != null) {
                partner.getUser().setPhone(request.getPhone().trim());
            }
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            partner.setEmail(request.getEmail().trim());
            if (partner.getUser() != null) {
                partner.getUser().setEmail(request.getEmail().trim());
            }
        }
        if (request.getVehicleNo() != null && !request.getVehicleNo().trim().isEmpty()) {
            partner.setVehicleNo(request.getVehicleNo().trim());
            if (partner.getAgent() != null) {
                partner.getAgent().setVehicleNo(request.getVehicleNo().trim());
            }
        }
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (partner.getUser() != null) {
                partner.getUser().setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
            }
        }
        if (request.getIsActive() != null) {
            partner.setIsActive(request.getIsActive());
            if (partner.getAgent() != null) {
                partner.getAgent().setIsAvailable(request.getIsActive());
            }
        }

        if (partner.getUser() != null) {
            userRepository.save(partner.getUser());
        }
        if (partner.getAgent() != null) {
            deliveryAgentRepository.save(partner.getAgent());
        }
        partner = deliveryPartnerRepository.save(partner);
        return mapToDTO(partner);
    }

    @Override
    @Transactional
    public DeliveryPartnerResponseDTO toggleActiveStatus(UUID id) {
        DeliveryPartner partner = getPartnerById(id);
        partner = ensureLinkedAgent(partner);
        boolean next = !Boolean.TRUE.equals(partner.getIsActive());
        partner.setIsActive(next);
        if (partner.getAgent() != null) {
            partner.getAgent().setIsAvailable(next);
            deliveryAgentRepository.save(partner.getAgent());
        }
        partner = deliveryPartnerRepository.save(partner);
        return mapToDTO(partner);
    }

    @Override
    @Transactional
    public List<DeliveryPartnerResponseDTO> getActiveDeliveryPartners() {
        return deliveryPartnerRepository.findByIsActiveTrue().stream()
                .map(p -> mapToDTO(ensureLinkedAgent(p)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDeliveryPartner(UUID id) {
        DeliveryPartner partner = getPartnerById(id);
        if (partner.getAgent() != null) {
            deliveryAgentRepository.delete(partner.getAgent());
        }
        if (partner.getUser() != null) {
            userRepository.delete(partner.getUser());
        }
        deliveryPartnerRepository.delete(partner);
    }

    @Override
    @Transactional
    public DeliveryAgent getOrCreateAgentForPartner(DeliveryPartner partner) {
        partner = ensureLinkedAgent(partner);
        return partner.getAgent();
    }

    /**
     * Lazy migration: guarantees that any existing partner in the DB automatically
     * gets a User (with role DELIVERY_AGENT) and DeliveryAgent record so they can log in.
     */
    private DeliveryPartner ensureLinkedAgent(DeliveryPartner partner) {
        if (partner == null) {
            return null;
        }

        boolean dirty = false;
        final String partnerName = partner.getName() != null ? partner.getName() : "Rider";
        final String partnerVehicle = partner.getVehicleNo() != null ? partner.getVehicleNo() : "UP16-RIDER";
        final boolean partnerActive = partner.getIsActive() != null ? partner.getIsActive() : true;
        String cleanPhone = partner.getPhone() != null ? partner.getPhone().trim() : "9999999999";
        String cleanEmail = partner.getEmail() != null && !partner.getEmail().trim().isEmpty()
                ? partner.getEmail().trim()
                : cleanPhone + "@rider.emistore.in";

        User user = partner.getUser();
        if (user == null) {
            user = userRepository.findByEmail(cleanEmail)
                    .or(() -> userRepository.findByPhone(cleanPhone))
                    .orElseGet(() -> User.builder()
                            .name(partnerName)
                            .email(cleanEmail)
                            .phone(cleanPhone)
                            .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                            .role(Role.DELIVERY_AGENT)
                            .status(UserStatus.ACTIVE)
                            .createdAt(LocalDateTime.now())
                            .build());

            user.setRole(Role.DELIVERY_AGENT);
            user = userRepository.save(user);
            partner.setUser(user);
            partner.setEmail(user.getEmail());
            dirty = true;
        }

        final User finalUser = user;
        DeliveryAgent agent = partner.getAgent();
        if (agent == null) {
            agent = deliveryAgentRepository.findByUserId(user.getId())
                    .orElseGet(() -> DeliveryAgent.builder()
                            .user(finalUser)
                            .vehicleNo(partnerVehicle)
                            .isAvailable(partnerActive)
                            .build());

            agent = deliveryAgentRepository.save(agent);
            partner.setAgent(agent);
            dirty = true;
        }

        if (dirty) {
            partner = deliveryPartnerRepository.save(partner);
        }
        return partner;
    }

    private DeliveryPartner getPartnerById(UUID id) {
        return deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Partner not found with id: " + id));
    }

    private DeliveryPartnerResponseDTO mapToDTO(DeliveryPartner partner) {
        String loginUsername = partner.getEmail() != null && !partner.getEmail().isBlank()
                ? partner.getEmail()
                : partner.getPhone();

        return DeliveryPartnerResponseDTO.builder()
                .id(partner.getId())
                .name(partner.getName())
                .phone(partner.getPhone())
                .email(partner.getEmail())
                .vehicleNo(partner.getVehicleNo())
                .isActive(partner.getIsActive())
                .userId(partner.getUser() != null ? partner.getUser().getId() : null)
                .agentId(partner.getAgent() != null ? partner.getAgent().getId() : null)
                .loginUsername(loginUsername)
                .createdAt(partner.getCreatedAt())
                .build();
    }
}
