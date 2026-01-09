package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.badge.dto.BadgeResponse;
import com.app.replant.domain.badge.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Badge", description = "뱃지 API")
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "내 유효 뱃지 목록 조회")
    @GetMapping
    public ApiResponse<List<BadgeResponse>> getMyBadges(
            @AuthenticationPrincipal Long userId) {
        List<BadgeResponse> badges = badgeService.getMyBadges(userId);
        return ApiResponse.success(badges);
    }

    @Operation(summary = "뱃지 히스토리 조회 (만료된 뱃지 포함)")
    @GetMapping("/history")
    public ApiResponse<Page<BadgeResponse>> getBadgeHistory(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BadgeResponse> history = badgeService.getBadgeHistory(userId, pageable);
        return ApiResponse.success(history);
    }

    @Operation(summary = "특정 미션에 대한 유효 뱃지 보유 여부 확인")
    @GetMapping("/check/{missionId}")
    public ApiResponse<Map<String, Boolean>> checkBadgeForMission(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long missionId) {
        boolean hasBadge = badgeService.hasValidBadgeForMission(userId, missionId);
        return ApiResponse.success(Map.of("hasBadge", hasBadge));
    }
}
