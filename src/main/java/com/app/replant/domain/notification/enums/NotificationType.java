package com.app.replant.domain.notification.enums;

/**
 * 알림 타입을 정의하는 Enum
 */
public enum NotificationType {
    // 미션 관련
    MISSION_ASSIGNED,           // 미션 할당됨

    // 커뮤니티 게시글 관련
    COMMENT,                    // 내 게시글에 댓글
    REPLY,                      // 내 댓글에 대댓글
    LIKE,                       // 내 게시글에 좋아요

    // 인증글 관련
    VERIFICATION_COMMENT,       // 내 인증글에 댓글
    VERIFICATION_REPLY,         // 내 인증글 댓글에 대댓글
    VERIFICATION_APPROVED,      // 인증 승인됨
    VERIFICATION_REJECTED,      // 인증 거절됨
    VOTE,                       // 인증글에 투표

    // 다이어리 관련
    DIARY,                      // 다이어리 알림

    // 추천 관련
    RECOMMENDATION,             // 사용자 추천

    // 채팅 관련
    CHAT_MESSAGE,               // 새 채팅 메시지

    // 기타
    SYSTEM,                     // 시스템 알림
    CUSTOM                      // 커스텀 알림
}
