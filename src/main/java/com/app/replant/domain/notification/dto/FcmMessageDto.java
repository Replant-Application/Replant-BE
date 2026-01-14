package com.app.replant.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * FCM 전송 Format DTO
 * FCM API에 실제 전송될 데이터의 형식을 정의합니다.
 * 
 * 기본적으로 메시지 단건 전송을 위해서는 아래와 같은 정보를 포함하여서 전송하여야 합니다:
 * POST https://fcm.googleapis.com/v1/projects/{project-id}/messages:send
 * 
 * {
 *    "message":{
 *       "token":"bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1...",
 *       "notification":{
 *         "body":"This is an FCM notification message!",
 *         "title":"FCM Message"
 *       }
 *    }
 * }
 *
 * @author : lee
 * @fileName : FcmMessageDto
 * @since : 2/21/24
 */
@Getter
@Builder
public class FcmMessageDto {
    
    /**
     * 메시지 검증만 수행할지 여부 (실제 전송하지 않음)
     */
    private boolean validateOnly;
    
    /**
     * FCM 메시지 내용
     */
    private FcmMessageDto.Message message;

    @Builder
    @AllArgsConstructor
    @Getter
    public static class Message {
        /**
         * 알림 정보
         */
        private FcmMessageDto.Notification notification;
        
        /**
         * 데이터 페이로드 (앱이 포그라운드에 있을 때도 알림을 표시하기 위해 필요)
         */
        private Map<String, String> data;
        
        /**
         * 대상 디바이스의 FCM 토큰
         */
        private String token;
    }

    @Builder
    @AllArgsConstructor
    @Getter
    public static class Notification {
        /**
         * 알림 제목
         */
        private String title;
        
        /**
         * 알림 내용
         */
        private String body;
        
        /**
         * 알림 이미지 URL (선택사항)
         */
        private String image;
    }
}
