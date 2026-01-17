package com.app.replant.domain.usermission.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 기상 미션 상태 조회 응답
 */
@Getter
@Builder
public class WakeUpMissionStatusResponse {
    
    private Long userMissionId;
    private LocalDateTime assignedAt;
    private Long timeRemaining;  // 초 단위 (남은 시간)
    private Boolean canVerify;   // 인증 가능 여부
    private String message;      // 상태 메시지
    
    public static WakeUpMissionStatusResponse from(Long userMissionId, LocalDateTime assignedAt, 
                                                   Long timeRemaining, Boolean canVerify, String message) {
        return WakeUpMissionStatusResponse.builder()
                .userMissionId(userMissionId)
                .assignedAt(assignedAt)
                .timeRemaining(timeRemaining)
                .canVerify(canVerify)
                .message(message)
                .build();
    }
}
