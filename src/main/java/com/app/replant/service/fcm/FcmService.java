package com.app.replant.service.fcm;

import com.app.replant.domain.notification.dto.FcmSendDto;

import java.io.IOException;

/**
 * FCM SERVICE
 *
 * @author : lee
 * @fileName : FcmService
 * @since : 2/21/24
 */
public interface FcmService {

    /**
     * FCM 푸시 메시지 전송
     * 
     * @param fcmSendDto FCM 전송 요청 DTO
     * @return 전송 성공 여부 (1: 성공, 0: 실패)
     * @throws IOException IO 예외
     */
    int sendMessageTo(FcmSendDto fcmSendDto) throws IOException;

    /**
     * 특정 사용자에게 FCM 푸시 알림 전송
     *
     * @param userId       수신자 ID
     * @param notification 알림 엔티티
     * @return 전송 성공 여부
     */
    boolean sendNotification(Long userId, com.app.replant.domain.notification.entity.Notification notification);

    /**
     * FCM 알림 전송 (재시도 로직 포함)
     *
     * @param userId       수신자 ID
     * @param notification 알림 엔티티
     * @return 전송 성공 여부
     */
    boolean sendNotificationWithRetry(Long userId, com.app.replant.domain.notification.entity.Notification notification);

    /**
     * 커스텀 FCM 알림 전송
     *
     * @param userId 수신자 ID
     * @param title  제목
     * @param body   내용
     * @param data   추가 데이터
     * @return 전송 성공 여부
     */
    boolean sendCustomNotification(Long userId, String title, String body, java.util.Map<String, String> data);
}
