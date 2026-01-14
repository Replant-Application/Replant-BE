package com.app.replant.domain.notification.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 모바일에서 전달받은 객체
 * FCM 푸시 알림 전송을 위한 요청 DTO
 *
 * @author : lee
 * @fileName : FcmSendDto
 * @since : 2/21/24
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmSendDto {
    
    /**
     * 디바이스 기기에서 발급받은 FCM 토큰 값
     * 디바이스로 전송하기 위해서는 전송 주체가 되는 디바이스 기기가 필요하기에 해당 내용이 들어갑니다.
     */
    private String token;

    /**
     * 디바이스 기기로 전송하려는 푸시메시지의 제목
     */
    private String title;

    /**
     * 디바이스 기기로 전송하려는 푸시메시지의 내용
     */
    private String body;

    @Builder(toBuilder = true)
    public FcmSendDto(String token, String title, String body) {
        this.token = token;
        this.title = title;
        this.body = body;
    }
}
