package com.app.replant.domain.usermission.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 돌발 미션 인증 요청
 * - 기상 미션: 단순 버튼 클릭 (시간 제한 10분)
 * - 식사 미션: 게시글 작성으로 인증 (postId 필요)
 */
@Getter
@NoArgsConstructor
public class VerifySpontaneousMissionRequest {
    
    // 식사 미션의 경우 게시글 ID (기상 미션은 null)
    private Long postId;
}
