package com.app.replant.domain.notification.service;

import com.app.replant.domain.notification.dto.NotificationResponse;
import com.app.replant.domain.notification.entity.Notification;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.repository.NotificationRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.app.replant.service.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseService sseService;

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

    /**
     * 알림 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = findNotificationByIdAndUserId(notificationId, userId);
        notification.softDelete();
    }

    private Notification findNotificationByIdAndUserId(Long notificationId, Long userId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    /**
     * 알림 생성 + DB 저장 + SSE 실시간 전송
     * @param user 수신자
     * @param type 알림 타입
     * @param title 알림 제목
     * @param content 알림 내용
     * @param referenceType 참조 타입 (nullable)
     * @param referenceId 참조 ID (nullable)
     * @return 생성된 알림 엔티티
     */
    @Transactional
    public Notification createAndPushNotification(User user, String type, String title,
                                                   String content, String referenceType, Long referenceId) {
        // 1. 알림 생성 및 DB 저장
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("알림 저장 완료 - userId: {}, type: {}, title: {}", user.getId(), type, title);

        // 2. SSE 연결되어 있으면 실시간 전송
        boolean sent = sseService.sendNotification(user.getId(), saved);
        if (!sent) {
            log.debug("SSE 미연결 상태 - userId: {}, 알림은 DB에 저장됨", user.getId());
        }

        return saved;
    }

    /**
     * 간편 알림 생성 (참조 없음)
     */
    @Transactional
    public Notification createAndPushNotification(User user, String type, String title, String content) {
        return createAndPushNotification(user, type, title, content, null, null);
    }

    // ============ NotificationType enum을 사용하는 메서드들 ============

    /**
     * 알림 생성 + DB 저장 + SSE 실시간 전송 (enum 버전)
     * @param user 수신자
     * @param type 알림 타입 (NotificationType enum)
     * @param title 알림 제목
     * @param content 알림 내용
     * @param referenceType 참조 타입 (nullable)
     * @param referenceId 참조 ID (nullable)
     * @return 생성된 알림 엔티티
     */
    @Transactional
    public Notification createAndPushNotification(User user, NotificationType type, String title,
                                                   String content, String referenceType, Long referenceId) {
        return createAndPushNotification(user, type.name(), title, content, referenceType, referenceId);
    }

    /**
     * 간편 알림 생성 (enum 버전, 참조 없음)
     */
    @Transactional
    public Notification createAndPushNotification(User user, NotificationType type, String title, String content) {
        return createAndPushNotification(user, type.name(), title, content, null, null);
    }
}
