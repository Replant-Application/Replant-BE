package com.app.replant.domain.usermission.enums;

public enum UserMissionStatus {
    ASSIGNED,   // 할당됨
    PENDING,    // 인증 대기중
    COMPLETED,  // 완료
    EXPIRED,    // 만료
    FAILED      // 실패 (데드라인 내 미완료)
}
