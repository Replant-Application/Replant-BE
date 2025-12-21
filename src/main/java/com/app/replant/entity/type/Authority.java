package com.app.replant.entity.type;

/**
 * 사용자 권한 타입
 */
public enum Authority {
    USER("ROLE_USER"),    // 일반 사용자
    ADMIN("ROLE_ADMIN");  // 관리자

    private final String securityRole;

    Authority(String securityRole) {
        this.securityRole = securityRole;
    }

    public String getSecurityRole() {
        return securityRole;
    }
}
