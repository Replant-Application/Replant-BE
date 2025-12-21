package com.app.replant.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 관리자 리포트 알림 요청 DTO
 */
@Schema(description = "관리자 리포트 알림 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReportNotificationRequestDto {

    @Schema(description = "회원 ID (이메일)", example = "user@example.com")
    private String memberId;

    @Schema(description = "알림 제목", example = "월간 리포트 발송")
    private String title;

    @Schema(description = "알림 내용", example = "이번 달 활동 리포트가 준비되었습니다.")
    private String content;

    @Schema(description = "대상 사용자 ID (null이면 전체 발송)", example = "1")
    private Long targetUserId;

    @Schema(description = "리포트 타입", example = "MONTHLY")
    private String reportType;
}
