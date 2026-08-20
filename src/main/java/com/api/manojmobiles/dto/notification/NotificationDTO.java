package com.api.manojmobiles.dto.notification;

import com.api.manojmobiles.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private UUID id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
