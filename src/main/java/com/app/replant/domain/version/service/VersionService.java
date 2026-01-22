package com.app.replant.domain.version.service;

import com.app.replant.domain.version.dto.VersionCheckResponse;

/**
 * 버전 체크 서비스 인터페이스
 */
public interface VersionService {
    /**
     * 버전 체크
     * @param currentVersion 현재 앱 버전
     * @param minVersion 최소 버전
     * @param latestVersion 최신 버전
     * @param storeUrl 스토어 URL
     * @param message 업데이트 메시지
     * @return 버전 체크 결과
     */
    VersionCheckResponse checkVersion(String currentVersion, String minVersion, 
                                     String latestVersion, String storeUrl, String message);
}
