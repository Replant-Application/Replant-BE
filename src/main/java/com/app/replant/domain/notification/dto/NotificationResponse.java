package com.app.replant.domain.notification.dto;

import com.app.replant.domain.notification.entity.Notification;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String referenceType;
    private Long referenceId;
    private Long userMissionId; // USER_MISSION 타입일 때 referenceId와 동일한 값 (프론트엔드 호환성)
    private Boolean isRead;
    /** 서버(Asia/Seoul) 기준으로 +09:00 포함 직렬화. null이면 JSON에서 제외되어 직렬화 예외 방지 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        NotificationResponse.NotificationResponseBuilder builder = NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt());
        
        // USER_MISSION 타입인 경우 userMissionId 필드도 설정 (프론트엔드 호환성)
        if ("USER_MISSION".equals(notification.getReferenceType()) && notification.getReferenceId() != null) {
            builder.userMissionId(notification.getReferenceId());
        }
        
        return builder.build();
    }
}
