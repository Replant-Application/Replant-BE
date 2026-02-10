package com.app.replant.domain.version.service;

import com.app.replant.domain.version.dto.VersionCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 버전 체크 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersionServiceImpl implements VersionService {

    @Override
    public VersionCheckResponse checkVersion(String currentVersion, String minVersion,
                                            String latestVersion, String storeUrl, String message) {
        // null/빈 값 방어: NPE 및 500 방지 (클라이언트 오류 시에도 서버는 200으로 응답)
        String current = (currentVersion != null && !currentVersion.isBlank()) ? currentVersion : "0.0.0";
        log.info("[VersionService] 버전 비교 - current: {}, min: {}, latest: {}", 
                current, minVersion, latestVersion);

        // 버전 비교
        boolean isRequired = compareVersions(current, minVersion) < 0;
        boolean isRecommended = compareVersions(current, latestVersion) < 0;

        // 강제 업데이트가 필요하면 선택 업데이트는 false
        if (isRequired) {
            isRecommended = false;
        }

        return VersionCheckResponse.builder()
                .isRequired(isRequired)
                .isRecommended(isRecommended)
                .message(message)
                .storeUrl(storeUrl)
                .minVersion(minVersion)
                .latestVersion(latestVersion)
                .build();
    }

    /**
     * 버전 비교
     * @param current 현재 버전
     * @param target 비교 대상 버전
     * @return current < target이면 -1, 같으면 0, current > target이면 1
     */
    private int compareVersions(String current, String target) {
        if (current == null || current.isBlank()) {
            current = "0.0.0";
        }
        if (target == null || target.isBlank()) {
            target = "0.0.0";
        }
        String[] currentParts = current.split("\\.");
        String[] targetParts = target.split("\\.");

        int maxLength = Math.max(currentParts.length, targetParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentPart = parseVersionPart(currentParts, i);
            int targetPart = parseVersionPart(targetParts, i);

            if (currentPart < targetPart) {
                return -1;
            } else if (currentPart > targetPart) {
                return 1;
            }
        }

        return 0;
    }

    /** 숫자가 아닌 부분은 0으로 처리 (500 방지) */
    private int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length || parts[index] == null || parts[index].isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
