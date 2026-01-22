package com.app.replant.domain.version.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 버전 체크 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionCheckResponse {
    private boolean isRequired;      // 강제 업데이트 필요 여부
    private boolean isRecommended;  // 선택 업데이트 권장 여부
    private String message;          // 업데이트 안내 메시지
    private String storeUrl;         // 스토어 URL
    private String minVersion;       // 최소 버전
    private String latestVersion;    // 최신 버전
}
