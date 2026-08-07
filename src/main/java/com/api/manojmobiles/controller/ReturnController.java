package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.returnreq.CreateReturnRequestDTO;
import com.api.manojmobiles.dto.returnreq.ReturnRequestResponseDTO;
import com.api.manojmobiles.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/returns")
@RequiredArgsConstructor
@Tag(name = "Return (Customer)")
@PreAuthorize("hasRole('CUSTOMER')")
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    @Operation(summary = "Submit a return request")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> createReturnRequest(
            Principal principal,
            @Valid @RequestBody CreateReturnRequestDTO request) {
        ReturnRequestResponseDTO response = returnService.createReturnRequest(principal.getName(), request);
        return new ResponseEntity<>(ApiResponse.success("Return request created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List user return requests")
    public ResponseEntity<ApiResponse<Page<ReturnRequestResponseDTO>>> getUserReturnRequests(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReturnRequestResponseDTO> responses = returnService.getUserReturnRequests(principal.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Return requests fetched successfully", responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get return request details")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> getReturnRequestById(
            Principal principal,
            @PathVariable UUID id) {
        ReturnRequestResponseDTO response = returnService.getReturnRequestById(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Return request details fetched successfully", response));
    }
}
