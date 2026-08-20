package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.notification.NotificationDTO;
import com.api.manojmobiles.entity.Notification;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.NotificationType;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.NotificationRepository;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(User user, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        notificationRepository.save(notification);
    }

    public Page<NotificationDTO> getUserNotifications(String username, Pageable pageable) {
        User user = getUser(username);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::mapToDTO);
    }

    public long getUnreadCount(String username) {
        User user = getUser(username);
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(String username, UUID notificationId) {
        User user = getUser(username);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
                
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found"); // Avoid access denied to prevent probing
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = getUser(username);
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(user.getId());
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        
        notificationRepository.saveAll(unreadNotifications);
    }

    private User getUser(String username) {
        return userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private NotificationDTO mapToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
