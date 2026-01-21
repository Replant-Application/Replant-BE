package com.app.replant.global.infrastructure.admin.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.mission.dto.MissionRequest;
import com.app.replant.domain.mission.dto.MissionResponse;
import com.app.replant.domain.mission.enums.MissionCategory;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Admin Mission", description = "관리자 미션 관리 API (ADMIN 권한 필요)")
@RestController
@RequestMapping("/api/admin/missions")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "JWT Token")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMissionController {

    private final MissionService missionService;

    @Operation(summary = "전체 미션 목록 조회 (관리자)")
    @GetMapping
    public ApiResponse<Page<MissionResponse>> getAllMissions(
            @RequestParam(required = false) MissionCategory category,
            @RequestParam(required = false) VerificationType verificationType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("관리자 - 전체 미션 조회");
        Page<MissionResponse> missions = missionService.getMissions(category, verificationType, pageable);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "미션 상세 조회 (관리자)")
    @GetMapping("/{missionId}")
    public ApiResponse<MissionResponse> getMission(@PathVariable Long missionId) {
        log.info("관리자 - 미션 상세 조회: {}", missionId);
        MissionResponse mission = missionService.getMission(missionId);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "미션 생성", description = "새로운 시스템 미션을 생성합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionResponse> createMission(@RequestBody @Valid MissionRequest request) {
        log.info("관리자 - 미션 생성: {}", request.getTitle());
        MissionResponse mission = missionService.createMission(request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "미션 수정")
    @PutMapping("/{missionId}")
    public ApiResponse<MissionResponse> updateMission(
            @PathVariable Long missionId,
            @RequestBody @Valid MissionRequest request) {
        log.info("관리자 - 미션 수정: {}", missionId);
        MissionResponse mission = missionService.updateMission(missionId, request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "미션 삭제")
    @DeleteMapping("/{missionId}")
    public ApiResponse<Map<String, String>> deleteMission(@PathVariable Long missionId) {
        log.info("관리자 - 미션 삭제: {}", missionId);
        missionService.deleteMission(missionId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "미션이 삭제되었습니다.");
        return ApiResponse.success(result);
    }

    @Operation(summary = "미션 활성화/비활성화 토글")
    @PatchMapping("/{missionId}/toggle-active")
    public ApiResponse<MissionResponse> toggleMissionActive(
            @PathVariable Long missionId,
            @RequestParam Boolean isActive) {
        log.info("관리자 - 미션 활성화 토글: {}, isActive={}", missionId, isActive);
        MissionResponse mission = missionService.toggleMissionActive(missionId, isActive);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "미션 대량 등록", description = "JSON 배열로 여러 미션을 한 번에 등록합니다")
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> bulkCreateMissions(@RequestBody @Valid List<MissionRequest> requests) {
        log.info("관리자 - 미션 대량 등록: {}개", requests.size());
        List<MissionResponse> createdMissions = missionService.bulkCreateMissions(requests);

        Map<String, Object> result = new HashMap<>();
        result.put("message", String.format("%d개의 미션이 등록되었습니다.", createdMissions.size()));
        result.put("count", createdMissions.size());
        result.put("missions", createdMissions);

        return ApiResponse.success(result);
    }
}
