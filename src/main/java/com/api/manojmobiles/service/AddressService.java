package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.address.AddressRequestDTO;
import com.api.manojmobiles.dto.address.AddressResponseDTO;
import com.api.manojmobiles.entity.Address;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.AddressRepository;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    private static final int MAX_ADDRESSES_PER_USER = 10;

    @Transactional(readOnly = true)
    public List<AddressResponseDTO> getUserAddresses(String identifier) {
        User user = getUserByEmailOrPhone(identifier);
        return addressRepository.findByUserId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressResponseDTO getAddressById(UUID addressId, String identifier) {
        User user = getUserByEmailOrPhone(identifier);
        Address address = getAddressAndVerifyOwnership(addressId, user.getId());
        return mapToDTO(address);
    }

    @Transactional
    public AddressResponseDTO createAddress(String identifier, AddressRequestDTO request) {
        User user = getUserByEmailOrPhone(identifier);

        List<Address> existingAddresses = addressRepository.findByUserId(user.getId());
        if (existingAddresses.size() >= MAX_ADDRESSES_PER_USER) {
            throw new BadRequestException("You can only have up to " + MAX_ADDRESSES_PER_USER + " addresses.");
        }

        boolean isFirstAddress = existingAddresses.isEmpty();
        boolean isDefault = request.getIsDefault() != null ? request.getIsDefault() : isFirstAddress;

        if (isDefault && !isFirstAddress) {
            resetDefaultAddress(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .lat(request.getLat())
                .lng(request.getLng())
                .isDefault(isDefault)
                .build();

        Address savedAddress = addressRepository.save(address);
        return mapToDTO(savedAddress);
    }

    @Transactional
    public AddressResponseDTO updateAddress(UUID addressId, String identifier, AddressRequestDTO request) {
        User user = getUserByEmailOrPhone(identifier);
        Address address = getAddressAndVerifyOwnership(addressId, user.getId());

        boolean newIsDefault = request.getIsDefault() != null ? request.getIsDefault() : address.getIsDefault();

        if (newIsDefault && !address.getIsDefault()) {
            resetDefaultAddress(user.getId());
        }

        address.setLabel(request.getLabel());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLat(request.getLat());
        address.setLng(request.getLng());
        
        // If it's the only address, it must remain default
        List<Address> existingAddresses = addressRepository.findByUserId(user.getId());
        if (existingAddresses.size() == 1) {
            address.setIsDefault(true);
        } else {
            address.setIsDefault(newIsDefault);
        }

        Address updatedAddress = addressRepository.save(address);
        return mapToDTO(updatedAddress);
    }

    @Transactional
    public void deleteAddress(UUID addressId, String identifier) {
        User user = getUserByEmailOrPhone(identifier);
        Address address = getAddressAndVerifyOwnership(addressId, user.getId());

        addressRepository.delete(address);

        // If we deleted the default address, make another one default if it exists
        if (address.getIsDefault()) {
            List<Address> remainingAddresses = addressRepository.findByUserId(user.getId());
            if (!remainingAddresses.isEmpty()) {
                Address newDefault = remainingAddresses.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    private User getUserByEmailOrPhone(String identifier) {
        return userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByPhone(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private Address getAddressAndVerifyOwnership(UUID addressId, UUID userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have permission to access this address");
        }
        return address;
    }

    private void resetDefaultAddress(UUID userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(oldDefault -> {
            oldDefault.setIsDefault(false);
            addressRepository.save(oldDefault);
        });
    }

    private AddressResponseDTO mapToDTO(Address address) {
        return AddressResponseDTO.builder()
                .id(address.getId())
                .label(address.getLabel())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .isDefault(address.getIsDefault())
                .build();
    }
}
