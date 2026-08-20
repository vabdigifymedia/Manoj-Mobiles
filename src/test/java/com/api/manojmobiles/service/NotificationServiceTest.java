package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.notification.NotificationDTO;
import com.api.manojmobiles.entity.Notification;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.NotificationType;
import com.api.manojmobiles.repository.NotificationRepository;
import com.api.manojmobiles.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");

        testNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .title("Test Notification")
                .message("Test Message")
                .type(NotificationType.SYSTEM)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateNotification() {
        notificationService.createNotification(testUser, "Order Placed", "Message", NotificationType.ORDER_UPDATE);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals("Order Placed", saved.getTitle());
        assertEquals("Message", saved.getMessage());
        assertEquals(NotificationType.ORDER_UPDATE, saved.getType());
        assertFalse(saved.getIsRead());
        assertEquals(testUser, saved.getUser());
    }

    @Test
    void testGetUserNotifications() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(testUser.getId()), any()))
                .thenReturn(page);

        Page<NotificationDTO> result = notificationService.getUserNotifications("test@example.com", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Notification", result.getContent().get(0).getTitle());
    }

    @Test
    void testGetUnreadCount() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(notificationRepository.countByUserIdAndIsReadFalse(testUser.getId())).thenReturn(5L);

        long count = notificationService.getUnreadCount("test@example.com");
        assertEquals(5L, count);
    }

    @Test
    void testMarkAsRead() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(notificationRepository.findById(testNotification.getId())).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead("test@example.com", testNotification.getId());

        assertTrue(testNotification.getIsRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void testMarkAllAsRead() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByUserIdAndIsReadFalse(testUser.getId()))
                .thenReturn(List.of(testNotification));

        notificationService.markAllAsRead("test@example.com");

        assertTrue(testNotification.getIsRead());
        verify(notificationRepository).saveAll(anyList());
    }
}
