package com.api.manojmobiles.controller;

import com.api.manojmobiles.config.RedisProperties;
import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.auth.AuthResponseDTO;
import com.api.manojmobiles.dto.auth.CustomerLoginRequestDTO;
import com.api.manojmobiles.dto.auth.CustomerSignUpDTO;
import com.api.manojmobiles.dto.auth.SendEmailOtpRequestDTO;
import com.api.manojmobiles.dto.auth.CustomerEmailLoginDTO;
import com.api.manojmobiles.dto.auth.CustomerEmailSignUpDTO;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, token refresh, and password management")
public class AuthController {

    private final AuthService authService;

    private final RateLimiterService rateLimiterService;
    private final RedisProperties redisProperties;

    // Extract client IP, accounting for reverse proxies
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Operation(summary = "Customer Login", description = "Authenticates a customer and returns a JWT access token and refresh token")
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

    @Operation(summary = "Staff Login", description = "Authenticates a staff member and returns a JWT access token and refresh token")
    @PostMapping("/staff/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> authenticateStaff(
            @Valid @RequestBody StaffLoginRequestDTO loginRequest,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        rateLimiterService.checkRateLimit(
                "rate:login:" + ip,
                redisProperties.getRate().getLoginLimit(),
                redisProperties.getRate().getLoginWindow());

        AuthResponseDTO response = authService.authenticateStaff(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Staff Login successful", response));
    }

    @Operation(summary = "Customer Registration", description = "Registers a new customer account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> registerUser(
            @Valid @RequestBody CustomerSignUpDTO signUpRequest) {
        AuthResponseDTO response = authService.registerCustomer(signUpRequest);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }



    @Operation(summary = "Send OTP", description = "Sends an OTP to the customer's phone number")
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<String>> sendOtp(
            @Valid @RequestBody SendOtpRequestDTO requestBody,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        rateLimiterService.checkRateLimit(
                "rate:otp:" + ip,
                redisProperties.getRate().getOtpLimit(),
                redisProperties.getRate().getOtpWindow());

        String otp = authService.sendOtp(requestBody.getPhone());
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", otp));
    }

    // =========================================================================
    // [TEMPORARY - PINE LABS APPROVAL]
    // Reason: TD Digital SMS credits exhausted; added for PG review team.
    // Removal: Delete the 3 endpoints below once SMS is restored.
    // =========================================================================

    @Operation(summary = "Send OTP to Email", description = "Sends an OTP to customer's email address [TEMPORARY - PINE LABS APPROVAL]")
    @PostMapping("/send-email-otp")
    public ResponseEntity<ApiResponse<String>> sendEmailOtp(
            @Valid @RequestBody SendEmailOtpRequestDTO requestBody,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        rateLimiterService.checkRateLimit(
                "rate:email-otp:" + ip,
                redisProperties.getRate().getOtpLimit(),
                redisProperties.getRate().getOtpWindow());

        String otp = authService.sendEmailOtp(requestBody.getEmail());
        return ResponseEntity.ok(ApiResponse.success("OTP sent to email successfully", otp));
    }

    @Operation(summary = "Customer Login with Email OTP", description = "Authenticates a customer using email and OTP [TEMPORARY - PINE LABS APPROVAL]")
    @PostMapping("/login-email-otp")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> authenticateUserWithEmailOtp(
            @Valid @RequestBody CustomerEmailLoginDTO loginRequest,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        rateLimiterService.checkRateLimit(
                "rate:login:" + ip,
                redisProperties.getRate().getLoginLimit(),
                redisProperties.getRate().getLoginWindow());

        AuthResponseDTO response = authService.authenticateCustomerWithEmailOtp(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Customer Registration with Email OTP", description = "Registers a new customer account using Email OTP [TEMPORARY - PINE LABS APPROVAL]")
    @PostMapping("/register-email-otp")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> registerUserWithEmailOtp(
            @Valid @RequestBody CustomerEmailSignUpDTO signUpRequest) {
        AuthResponseDTO response = authService.registerCustomerWithEmailOtp(signUpRequest);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }



    @Operation(summary = "Forgot Password", description = "Initiates the password reset process by generating a reset token")
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
        // Return reset token in response payload for dev/staging testing
        return ResponseEntity.ok(ApiResponse.success("Password reset initiated", token));
    }

    @Operation(summary = "Reset Password", description = "Resets the user's password using the generated token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }



    @Operation(summary = "Refresh Token", description = "Generates a new access token using a valid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.refreshToken(request.getUserId(), request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @Operation(summary = "Logout", description = "Invalidates the current refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequestDTO request) {
        authService.logout(request.getUserId(), request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @Operation(summary = "Change Password", description = "Changes the logged-in user's password (requires valid JWT)", security = { @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth") })
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
