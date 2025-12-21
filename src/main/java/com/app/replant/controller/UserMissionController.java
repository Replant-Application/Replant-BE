package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.usermission.dto.*;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.service.UserMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "UserMission", description = "내 미션 API")
@RestController
@RequestMapping("/api/user-missions")
@RequiredArgsConstructor
public class UserMissionController {

    private final UserMissionService userMissionService;

    @Operation(summary = "내 미션 목록 조회")
    @GetMapping
    public ApiResponse<Page<UserMissionResponse>> getUserMissions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) UserMissionStatus status,
            @RequestParam(required = false) String missionType,
            @PageableDefault(size = 20, sort = "assignedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserMissionResponse> missions = userMissionService.getUserMissions(userId, status, missionType, pageable);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "내 미션 상세 조회")
    @GetMapping("/{userMissionId}")
    public ApiResponse<UserMissionResponse> getUserMission(
            @PathVariable Long userMissionId,
            @AuthenticationPrincipal Long userId) {
        UserMissionResponse mission = userMissionService.getUserMission(userMissionId, userId);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 추가")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserMissionResponse> addCustomMission(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid AddCustomMissionRequest request) {
        UserMissionResponse mission = userMissionService.addCustomMission(userId, request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "미션 인증 (GPS/TIME)")
    @PostMapping("/{userMissionId}/verify")
    public ApiResponse<VerifyMissionResponse> verifyMission(
            @PathVariable Long userMissionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VerifyMissionRequest request) {
        VerifyMissionResponse response = userMissionService.verifyMission(userMissionId, userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "미션 수행 이력 조회", description = "완료/실패 포함 전체 미션 이력 조회")
    @GetMapping("/history")
    public ApiResponse<Page<UserMissionResponse>> getMissionHistory(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserMissionResponse> history = userMissionService.getMissionHistory(userId, pageable);
        return ApiResponse.success(history);
    }
}
