package com.app.replant.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 관리자 일기 알림 요청 DTO
 */
@Schema(description = "관리자 일기 알림 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDiaryNotificationRequestDto {

    @Schema(description = "회원 ID (이메일)", example = "user@example.com")
    private String memberId;

    @Schema(description = "알림 제목", example = "일기 작성 알림")
    private String title;

    @Schema(description = "알림 내용", example = "오늘의 일기를 작성해주세요.")
    private String content;

    @Schema(description = "대상 사용자 ID (null이면 전체 발송)", example = "1")
    private Long targetUserId;
}
