package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.auth.AuthResponseDTO;
import com.api.manojmobiles.dto.auth.CreateStaffRequestDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;

    // ─── OTP ──────────────────────────────────────────────

    /**
     * Send OTP to a phone number.
     * Delegates to OtpService which stores in Redis with configurable TTL.
     */
    public void sendOtp(String phone) {
        // Verify phone exists (optional: remove this check to allow pre-registration OTP)
        userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with phone: " + phone));
        otpService.generateOtp(phone);
    }

    // ─── Customer Authentication ──────────────────────────

    public AuthResponseDTO authenticateCustomer(CustomerLoginRequestDTO loginRequest) {
        // Real OTP verification via Redis
        otpService.verifyOtp(loginRequest.getPhone(), loginRequest.getOtp());

        User user = userRepository.findByPhone(loginRequest.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with phone: " + loginRequest.getPhone()));

        String jwt = tokenProvider.generateToken(user.getPhone());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponseDTO.builder()
                .token(jwt)
                .refreshToken(refreshToken)
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

        String jwt = tokenProvider.generateToken(result.getPhone());
        String refreshToken = refreshTokenService.createRefreshToken(result.getId());

        return AuthResponseDTO.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .userId(result.getId())
                .name(result.getName())
                .role(result.getRole())
                .build();
    }

    // ─── Staff Authentication ─────────────────────────────

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

        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        boolean isDefaultCredentials = "admin@manojmobiles.com".equalsIgnoreCase(user.getEmail())
                && passwordEncoder.matches("Admin@123", user.getPasswordHash());

        return AuthResponseDTO.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .mustChangePassword(isDefaultCredentials)
                .build();
    }

    // ─── Admin: Create Staff / Delivery Agent ─────────────

    public User createStaffUser(CreateStaffRequestDTO requestDTO) {
        if (requestDTO.getRole() == Role.CUSTOMER) {
            throw new BadRequestException("This endpoint cannot create CUSTOMER roles.");
        }

        if (userRepository.findByEmail(requestDTO.getEmail()).isPresent()) {
            throw new BadRequestException("Email already in use!");
        }

        if (userRepository.findByPhone(requestDTO.getPhone()).isPresent()) {
            throw new BadRequestException("Phone Number already in use!");
        }

        User user = User.builder()
                .name(requestDTO.getName())
                .email(requestDTO.getEmail())
                .phone(requestDTO.getPhone())
                .passwordHash(passwordEncoder.encode(requestDTO.getPassword()))
                .role(requestDTO.getRole())
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    // ─── Refresh Token ────────────────────────────────────

    /**
     * Refresh the access token using a valid refresh token.
     * Validates the refresh token, generates a new access token,
     * and creates a new refresh token (rotation).
     */
    public AuthResponseDTO refreshToken(UUID userId, String refreshToken) {
        // Validate the existing refresh token
        refreshTokenService.validateRefreshToken(userId, refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Generate new access token
        String identifier = user.getEmail() != null ? user.getEmail() : user.getPhone();
        String newJwt = tokenProvider.generateToken(identifier);

        // Rotate refresh token (create new one, old one is overwritten)
        String newRefreshToken = refreshTokenService.createRefreshToken(userId);

        return AuthResponseDTO.builder()
                .token(newJwt)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    /**
     * Logout: delete the refresh token from Redis.
     */
    public void logout(UUID userId) {
        refreshTokenService.deleteRefreshToken(userId);
    }

    // ─── Password Reset ───────────────────────────────────

    /**
     * Initiate forgot password flow.
     * Generates a password reset token stored in Redis.
     *
     * @return the reset token (in production, this would be emailed to the user)
     */
    public String forgotPassword(String email) {
        return passwordResetService.createResetToken(email);
    }

    /**
     * Reset the password using the token from the forgot-password flow.
     */
    public void resetPassword(String token, String newPassword) {
        passwordResetService.resetPassword(token, newPassword);
    }
}
