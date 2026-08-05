package com.api.manojmobiles.controller;

import com.api.manojmobiles.config.RedisProperties;
import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.auth.AuthResponseDTO;
import com.api.manojmobiles.dto.auth.CustomerLoginRequestDTO;
import com.api.manojmobiles.dto.auth.CustomerSignUpDTO;
import com.api.manojmobiles.dto.auth.ForgetPasswordRequestDTO;
import com.api.manojmobiles.dto.auth.LogoutRequestDTO;
import com.api.manojmobiles.dto.auth.RefreshTokenRequestDTO;
import com.api.manojmobiles.dto.auth.ResetPasswordRequestDTO;
import com.api.manojmobiles.dto.auth.SendOtpRequestDTO;
import com.api.manojmobiles.dto.auth.ChangePasswordRequestDTO;
import com.api.manojmobiles.dto.auth.StaffLoginRequestDTO;
import com.api.manojmobiles.service.AuthService;

import com.api.manojmobiles.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final RateLimiterService rateLimiterService;
    private final RedisProperties redisProperties;

    // Helper to get client IP
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> authenticateUser(
            @Valid @RequestBody CustomerLoginRequestDTO loginRequest,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        rateLimiterService.checkRateLimit(
                "rate:login:" + ip,
                redisProperties.getRate().getLoginLimit(),
                redisProperties.getRate().getLoginWindow());

        AuthResponseDTO response = authService.authenticateCustomer(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/staff/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> authenticateStaff(
            @Valid @RequestBody StaffLoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.authenticateStaff(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Staff Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> registerUser(
            @Valid @RequestBody CustomerSignUpDTO signUpRequest) {
        AuthResponseDTO response = authService.registerCustomer(signUpRequest);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }

    // OTP Endpoints

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequestDTO requestBody,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        rateLimiterService.checkRateLimit(
                "rate:otp:" + ip,
                redisProperties.getRate().getOtpLimit(),
                redisProperties.getRate().getOtpWindow());

        authService.sendOtp(requestBody.getPhone());
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", null));
    }

    // Password Reset

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgetPasswordRequestDTO requestBody,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        rateLimiterService.checkRateLimit(
                "rate:forgot-pw:" + ip,
                redisProperties.getRate().getForgotPasswordLimit(),
                redisProperties.getRate().getForgotPasswordWindow());

        String token = authService.forgotPassword(requestBody.getEmail());
        // In production, send the token via email. Here we return it for testing
        // purposes.
        return ResponseEntity.ok(ApiResponse.success("Password reset initiated", token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    // Refresh & Logout

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.refreshToken(request.getUserId(), request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequestDTO request) {
        authService.logout(request.getUserId(), request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
