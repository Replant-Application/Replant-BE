package com.app.replant.domain.mission.enums;

public enum VerificationType {
    COMMUNITY, GPS, TIME,
    /** 기상 미션 등 버튼 클릭 인증 (실제 검증 시 TIME으로 처리) */
    BUTTON
}
