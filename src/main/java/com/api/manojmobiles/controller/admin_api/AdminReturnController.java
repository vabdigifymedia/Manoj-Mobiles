package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.returnreq.ReturnRequestResponseDTO;
import com.api.manojmobiles.dto.returnreq.UpdateReturnStatusRequestDTO;
import com.api.manojmobiles.entity.enums.ReturnStatus;
import com.api.manojmobiles.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
@Tag(name = "Return (Admin)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReturnController {

    private final ReturnService returnService;

    @GetMapping
    @Operation(summary = "List all return requests (filterable by status)")
    public ResponseEntity<ApiResponse<Page<ReturnRequestResponseDTO>>> getAllReturnRequests(
            @RequestParam(required = false) ReturnStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReturnRequestResponseDTO> responses = returnService.getAllReturnRequests(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Return requests fetched successfully", responses));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update return request status")
    public ResponseEntity<ApiResponse<ReturnRequestResponseDTO>> updateReturnStatus(
            Principal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReturnStatusRequestDTO request) {
        ReturnRequestResponseDTO response = returnService.updateReturnStatus(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Return request status updated successfully", response));
    }
}
