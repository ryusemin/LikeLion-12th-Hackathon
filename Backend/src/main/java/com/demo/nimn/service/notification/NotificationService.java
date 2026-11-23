package com.demo.nimn.service.notification;

import com.demo.nimn.dto.notification.NotificationDTO;
import com.demo.nimn.dto.notification.response.NotificationCountDTO;
import com.demo.nimn.enums.NotificationType;

import java.util.List;

public interface NotificationService {
    NotificationDTO markAsRead(Long notificationId, String ownerEmail);
    NotificationCountDTO markAllAsRead(String userEmail);
    NotificationCountDTO countUnreadNotifications(String userEmail);
    List<NotificationDTO> getAllNotificationsByUserEmail(String userEmail);

    NotificationCountDTO testNotification(NotificationType notificationType,
                                          String userEmail,
                                          String content,
                                          Long dailyReviewId);
}