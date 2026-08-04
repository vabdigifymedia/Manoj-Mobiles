package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.auth.AuthResponseDTO;
import com.api.manojmobiles.dto.auth.CustomerLoginRequestDTO;
import com.api.manojmobiles.dto.auth.CustomerSignUpDTO;
import com.api.manojmobiles.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> authenticateUser(@Valid @RequestBody CustomerLoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.authenticateCustomer(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/staff/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> authenticateStaff(@Valid @RequestBody com.api.manojmobiles.dto.auth.StaffLoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.authenticateStaff(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Staff Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> registerUser(@Valid @RequestBody CustomerSignUpDTO signUpRequest) {
        AuthResponseDTO response = authService.registerCustomer(signUpRequest);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }
}
