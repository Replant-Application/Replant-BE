package com.app.replant.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 마이 페이지 통계 정보 응답 DTO
 * 통계만 필요한 경우 대량 조회를 방지하기 위해 별도 API 제공
 */
@Getter
@Builder
public class UserStatsResponse {
    /**
     * 완료한 미션 수
     */
    private Long completedMissionsCount;

    /**
     * 작성한 게시글 수 (삭제되지 않은 것만)
     */
    private Long postsCount;

    /**
     * 작성한 인증글 수 (승인된 것만)
     */
    private Long approvedVerificationsCount;

    /**
     * 작성한 일기 수
     */
    private Long diariesCount;

    /**
     * 획득한 뱃지 수
     */
    private Long badgesCount;
}
