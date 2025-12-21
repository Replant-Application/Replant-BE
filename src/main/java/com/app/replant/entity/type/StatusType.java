package com.app.replant.entity.type;

/**
 * 사용자 계정 상태 타입
 */
public enum StatusType {
    ABLE,      // 활성
    UNABLE,    // 비활성 (legacy 호환)
    DISABLED,  // 비활성
    SUSPENDED  // 정지
}
