package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.custommission.dto.CustomMissionRequest;
import com.app.replant.domain.custommission.dto.CustomMissionResponse;
import com.app.replant.domain.custommission.service.CustomMissionService;
import com.app.replant.domain.mission.enums.VerificationType;
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

import java.util.HashMap;
import java.util.Map;

@Tag(name = "CustomMission", description = "커스텀 미션 API")
@RestController
@RequestMapping("/api/custom-missions")
@RequiredArgsConstructor
public class CustomMissionController {

    private final CustomMissionService customMissionService;

    @Operation(summary = "커스텀 미션 목록 조회")
    @GetMapping
    public ApiResponse<Page<CustomMissionResponse>> getCustomMissions(
            @RequestParam(required = false) VerificationType verificationType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CustomMissionResponse> missions = customMissionService.getCustomMissions(verificationType, pageable);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "커스텀 미션 상세 조회")
    @GetMapping("/{customMissionId}")
    public ApiResponse<CustomMissionResponse> getCustomMission(
            @PathVariable Long customMissionId) {
        CustomMissionResponse mission = customMissionService.getCustomMission(customMissionId);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomMissionResponse> createCustomMission(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CustomMissionRequest request) {
        CustomMissionResponse mission = customMissionService.createCustomMission(userId, request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 수정")
    @PutMapping("/{customMissionId}")
    public ApiResponse<CustomMissionResponse> updateCustomMission(
            @PathVariable Long customMissionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CustomMissionRequest request) {
        CustomMissionResponse mission = customMissionService.updateCustomMission(customMissionId, userId, request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 삭제")
    @DeleteMapping("/{customMissionId}")
    public ApiResponse<Map<String, String>> deleteCustomMission(
            @PathVariable Long customMissionId,
            @AuthenticationPrincipal Long userId) {
        customMissionService.deleteCustomMission(customMissionId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "커스텀 미션이 삭제되었습니다.");

        return ApiResponse.success(result);
    }
}
