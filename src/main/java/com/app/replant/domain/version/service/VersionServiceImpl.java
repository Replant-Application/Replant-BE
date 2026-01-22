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
        log.info("[VersionService] 버전 비교 - current: {}, min: {}, latest: {}", 
                currentVersion, minVersion, latestVersion);

        // 버전 비교
        boolean isRequired = compareVersions(currentVersion, minVersion) < 0;
        boolean isRecommended = compareVersions(currentVersion, latestVersion) < 0;

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
        String[] currentParts = current.split("\\.");
        String[] targetParts = target.split("\\.");

        int maxLength = Math.max(currentParts.length, targetParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int targetPart = i < targetParts.length ? Integer.parseInt(targetParts[i]) : 0;

            if (currentPart < targetPart) {
                return -1;
            } else if (currentPart > targetPart) {
                return 1;
            }
        }

        return 0;
    }
}
