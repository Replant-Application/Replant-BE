package com.app.replant.domain.notification.service;

import com.app.replant.domain.notification.dto.NotificationResponse;
import com.app.replant.domain.notification.entity.Notification;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.repository.NotificationRepository;
import com.app.replant.domain.notification.repository.RedisUserOnlineRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.app.replant.service.fcm.FcmService;
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
    private final RedisUserOnlineRepository redisUserOnlineRepository;
    private final UserRepository userRepository;
    private final SseService sseService;
    private final FcmService fcmService;

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
     * 알림 삭제 (Hard Delete)
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = findNotificationByIdAndUserId(notificationId, userId);
        notificationRepository.delete(notification);
    }

    private Notification findNotificationByIdAndUserId(Long notificationId, Long userId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    /**
     * 알림 생성 + DB 저장 + SSE/FCM 전송
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
        log.info("[알림] 저장 완료 - userId: {}, type: {}, title: {}", user.getId(), type, title);

        Long userId = user.getId();
        boolean isOnline = redisUserOnlineRepository.isOnline(userId);

        if (isOnline) {
            // 사용자가 접속 중 (Redis 기준 online)
            log.info("[알림] 사용자 온라인 상태 감지 - userId: {}, SSE 전송 시도", userId);
            
            // 1. SSE로 알림 전송 시도
            boolean sentViaSse = sseService.sendNotification(userId, saved);
            
            if (sentViaSse) {
                log.info("[알림] SSE 실시간 전송 성공 - userId: {}", userId);
            } else {
                // SSE 전송 실패 시 FCM으로 대체
                log.warn("[알림] SSE 전송 실패, FCM으로 대체 전송 시도 - userId: {}", userId);
                boolean sentViaFcm = fcmService.sendNotificationWithRetry(userId, saved);
                
                if (sentViaFcm) {
                    log.info("[알림] FCM 대체 전송 성공 - userId: {}", userId);
                } else {
                    log.warn("[알림] SSE/FCM 모두 실패. DB에만 저장됨 - userId: {}", userId);
                }
            }
        } else {
            // 사용자가 미접속 중 (Redis TTL 만료 또는 전송 실패)
            log.info("[알림] 사용자 오프라인 상태 감지 - userId: {}, FCM 푸시 알림 전송 시도", userId);
            
            // FCM을 통해 푸시 전송 (재시도 로직 포함)
            boolean sentViaFcm = fcmService.sendNotificationWithRetry(userId, saved);
            
            if (sentViaFcm) {
                log.info("[알림] FCM 푸시 알림 전송 성공 - userId: {}", userId);
            } else {
                log.warn("[알림] FCM 전송 실패. DB에만 저장됨 - userId: {}", userId);
            }
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

    // ============ 특정 알림 타입 전송 메서드 ============

    /**
     * 일기 알림 전송 (SSE + FCM)
     * @param user 수신자
     */
    @Transactional
    public void sendDiaryNotification(User user) {
        createAndPushNotification(
                user,
                NotificationType.DIARY,
                "일기 알림",
                "오늘 하루는 어떠셨나요? 일기를 작성해보세요."
        );
    }

    /**
     * 미션 배정 알림 전송 (SSE + FCM)
     * @param user 수신자
     * @param missionType 미션 타입
     * @param missionCount 배정된 미션 개수
     */
    @Transactional
    public void sendMissionNotification(User user, String missionType, int missionCount) {
        createAndPushNotification(
                user,
                NotificationType.MISSION_ASSIGNED,
                "미션 알림",
                String.format("%d개의 %s 미션이 배정되었습니다. 지금 확인해보세요!", missionCount, missionType)
        );
    }

    // ============ FCM 토큰 관리 (User 테이블) ============

    /**
     * FCM 토큰 등록/업데이트 (User 테이블 저장)
     * @param userId 사용자 ID
     * @param token FCM 토큰
     */
    @Transactional
    public void registerFcmToken(Long userId, String token) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // User 테이블에 FCM 토큰 저장 (기존 토큰이 있으면 자동으로 덮어씌워짐)
        user.updateFcmToken(token);
        log.info("[FCM] 토큰 User 테이블 저장 완료 - userId: {}", userId);
    }
}
