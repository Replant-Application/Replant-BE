package com.app.replant.domain.notification.service;

import com.app.replant.domain.notification.dto.NotificationResponse;
import com.app.replant.domain.notification.entity.Notification;
import com.app.replant.domain.notification.repository.NotificationRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<NotificationResponse> getNotifications(Long userId, Boolean isRead, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable)
                .map(NotificationResponse::from);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markNotificationAsRead(Long notificationId, Long userId) {
        Notification notification = findNotificationByIdAndUserId(notificationId, userId);
        notification.markAsRead();
    }

    @Transactional
    public int markAllNotificationsAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    private Notification findNotificationByIdAndUserId(Long notificationId, Long userId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
