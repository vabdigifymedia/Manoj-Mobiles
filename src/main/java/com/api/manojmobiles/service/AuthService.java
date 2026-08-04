package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.auth.AuthResponseDTO;
import com.api.manojmobiles.dto.auth.CustomerLoginRequestDTO;
import com.api.manojmobiles.dto.auth.CustomerSignUpDTO;
import com.api.manojmobiles.dto.auth.StaffLoginRequestDTO;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.Role;
import com.api.manojmobiles.entity.enums.UserStatus;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.UserRepository;
import com.api.manojmobiles.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public AuthResponseDTO authenticateCustomer(CustomerLoginRequestDTO loginRequest) {
        // Mock OTP Validation (e.g. "1234")
        if (!"1234".equals(loginRequest.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        User user = userRepository.findByPhone(loginRequest.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with phone: " + loginRequest.getPhone()));

        String jwt = tokenProvider.generateToken(user.getPhone());

        return AuthResponseDTO.builder()
                .token(jwt)
                .userId(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    public AuthResponseDTO registerCustomer(CustomerSignUpDTO signUpRequest) {
        if (userRepository.findByPhone(signUpRequest.getPhone()).isPresent()) {
            throw new BadRequestException("Phone Number already in use!");
        }

        User user = User.builder()
                .name(signUpRequest.getName())
                .phone(signUpRequest.getPhone())
                // No email, no password for customer
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User result = userRepository.save(user);

        // Usually we would trigger sendOtp() here. For now, we mock an auto-login after signup
        String jwt = tokenProvider.generateToken(result.getPhone());

        return AuthResponseDTO.builder()
                .token(jwt)
                .userId(result.getId())
                .name(result.getName())
                .role(result.getRole())
                .build();
    }

    public AuthResponseDTO authenticateStaff(StaffLoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getRole() == Role.CUSTOMER) {
            throw new BadRequestException("Customers cannot use this login endpoint.");
        }

        return AuthResponseDTO.builder()
                .token(jwt)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
