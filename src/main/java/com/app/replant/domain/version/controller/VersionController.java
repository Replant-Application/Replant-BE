package com.app.replant.domain.version.controller;

import com.app.replant.domain.version.dto.VersionCheckRequest;
import com.app.replant.domain.version.dto.VersionCheckResponse;
import com.app.replant.domain.version.service.VersionService;
import com.app.replant.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 앱 버전 체크 API
 * FCM으로 통일된 업데이트 알림 시스템
 */
@Tag(name = "버전 체크", description = "앱 버전 체크 및 업데이트 알림 API")
@RestController
@RequestMapping("/api/v1/version")
@RequiredArgsConstructor
@Slf4j
public class VersionController {

    private final VersionService versionService;

    @Value("${app.version.min:0.0.0}")
    private String minVersion;

    @Value("${app.version.latest:0.0.0}")
    private String latestVersion;

    @Value("${app.version.store-url:https://play.google.com/store/apps/details?id=com.anonymous.replantmobileapp}")
    private String storeUrl;

    @Value("${app.version.message:}")
    private String updateMessage;
    
    // 기본 메시지 (인코딩 문제 방지 - 한글 테스트)
    // 유니코드 이스케이프를 사용하여 인코딩 문제 방지
    // "더 나은 서비스 이용을 위해 업데이트가 필요합니다."
    private static final String DEFAULT_UPDATE_MESSAGE = "\uB354 \uB098\uC740 \uC11C\uBE44\uC2A4 \uC774\uC6A9\uC744 \uC704\uD574 \uC5C5\uB370\uC774\uD2B8\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4.";

    /**
     * 앱 버전 체크
     * 앱 시작 시 호출하여 업데이트 필요 여부 확인
     */
    @Operation(summary = "앱 버전 체크", description = "현재 앱 버전과 서버의 최소/최신 버전을 비교하여 업데이트 필요 여부를 반환합니다.")
    @PostMapping("/check")
    public ApiResponse<VersionCheckResponse> checkVersion(
            @RequestBody VersionCheckRequest request) {
        log.info("[Version] 버전 체크 요청 - currentVersion: {}", request.getCurrentVersion());
        
        // 항상 기본 메시지 사용 (인코딩 문제 방지)
        // application.properties의 기본값은 영어이므로, Java 코드의 한글 메시지를 우선 사용
        String message = DEFAULT_UPDATE_MESSAGE;
        
        VersionCheckResponse response = versionService.checkVersion(
                request.getCurrentVersion(),
                minVersion,
                latestVersion,
                storeUrl,
                message
        );
        
        log.info("[Version] 버전 체크 결과 - isRequired: {}, isRecommended: {}", 
                response.isRequired(), response.isRecommended());
        
        return ApiResponse.success(response);
    }
}
