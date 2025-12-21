package com.app.replant.domain.user.enums;

/**
 * 사용자 역할
 * - USER: 일반 사용자
 * - GRADUATE: 졸업자 (모든 미션을 완료한 사용자)
 * - CONTRIBUTOR: 기여자 (봉사자)
 * - ADMIN: 관리자
 */
public enum UserRole {
    USER,           // 일반 사용자
    GRADUATE,       // 졸업자
    CONTRIBUTOR,    // 기여자
    ADMIN           // 관리자
}
