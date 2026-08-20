package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.notification.NotificationDTO;
import com.api.manojmobiles.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification APIs", description = "Endpoints for user notifications")
@PreAuthorize("hasRole('CUSTOMER')")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get user notifications", description = "Fetches a paginated list of notifications for the logged-in user")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getUserNotifications(
            Authentication authentication,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationDTO> notifications = notificationService.getUserNotifications(authentication.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", notifications));
    }

    @Operation(summary = "Get unread count", description = "Returns the total number of unread notifications for the user")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        long count = notificationService.getUnreadCount(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", count));
    }

    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable UUID id) {
        notificationService.markAsRead(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for the logged-in user")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
