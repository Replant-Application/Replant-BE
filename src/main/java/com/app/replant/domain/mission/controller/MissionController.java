package com.app.replant.domain.mission.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.mission.dto.*;
import com.app.replant.domain.mission.enums.*;
import com.app.replant.domain.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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


@Tag(name = "Mission", description = "공식 미션 API")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @Operation(summary = "미션 목록 조회 (미션 도감용)", 
               description = "미션 도감에서 전체 미션을 조회합니다. 페이징 크기를 크게 설정하거나 size 파라미터로 조정 가능합니다.")
    @GetMapping
    public ApiResponse<Page<MissionResponse>> getMissions(
            @RequestParam(required = false) MissionCategory category,
            @RequestParam(required = false) VerificationType verificationType,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 1000, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionResponse> missions = missionService.getMissions(category, verificationType, pageable, userId);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "미션 도감 목록 조회 (collection 경로 지원)", 
               description = "/api/missions/collection은 /api/missions와 동일한 기능을 제공합니다. 전체 미션을 조회합니다.")
    @GetMapping("/collection")
    public ApiResponse<Page<MissionResponse>> getMissionsCollection(
            @RequestParam(required = false) MissionCategory category,
            @RequestParam(required = false) VerificationType verificationType,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 1000, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionResponse> missions = missionService.getMissions(category, verificationType, pageable, userId);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "사용자 맞춤 미션 목록 조회 (필터링)")
    @GetMapping("/filtered")
    public ApiResponse<Page<MissionResponse>> getFilteredMissions(
            @RequestParam(required = false) MissionCategory category,
            @RequestParam(required = false) VerificationType verificationType,
            @RequestParam(required = false) WorryType worryType,
            @RequestParam(required = false) AgeRange ageRange,
            @RequestParam(required = false) GenderType genderType,
            @RequestParam(required = false) RegionType regionType,
            @RequestParam(required = false) DifficultyLevel difficultyLevel,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionResponse> missions = missionService.getFilteredMissions(
                category, verificationType, worryType, ageRange, genderType, regionType, difficultyLevel, pageable, userId);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "공식 미션 검색", description = "제목/설명으로 공식 미션을 검색하고 필터링합니다.")
    @GetMapping("/search")
    public ApiResponse<Page<MissionResponse>> searchOfficialMissions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MissionCategory category,
            @RequestParam(required = false) VerificationType verificationType,
            @RequestParam(required = false) WorryType worryType,
            @RequestParam(required = false) AgeRange ageRange,
            @RequestParam(required = false) GenderType genderType,
            @RequestParam(required = false) RegionType regionType,
            @RequestParam(required = false) DifficultyLevel difficultyLevel,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionResponse> missions = missionService.searchOfficialMissions(
                keyword, category, verificationType, worryType, ageRange,
                genderType, regionType, difficultyLevel, pageable, userId);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "미션 상세 조회")
    @GetMapping("/{missionId:\\d+}")
    public ApiResponse<MissionResponse> getMission(
            @PathVariable Long missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        MissionResponse mission = missionService.getMission(missionId, userId);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "미션 리뷰 목록 조회")
    @GetMapping("/{missionId:\\d+}/reviews")
    public ApiResponse<Page<MissionReviewResponse>> getReviews(
            @PathVariable Long missionId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MissionReviewResponse> reviews = missionService.getReviews(missionId, pageable);
        return ApiResponse.success(reviews);
    }

    @Operation(summary = "미션 리뷰 작성")
    @PostMapping("/{missionId:\\d+}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionReviewResponse> createReview(
            @PathVariable Long missionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid MissionReviewRequest request) {
        MissionReviewResponse review = missionService.createReview(missionId, userId, request);
        return ApiResponse.success(review);
    }

    // ============ 커스텀 미션 CRUD API ============

    @Operation(summary = "커스텀 미션 목록 조회", description = "공개된 커스텀 미션 목록을 조회합니다.")
    @GetMapping("/custom")
    public ApiResponse<Page<MissionResponse>> getCustomMissions(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionResponse> missions = missionService.getCustomMissions(pageable, userId);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "커스텀 미션 검색", description = "제목/설명으로 커스텀 미션을 검색하고 필터링합니다.")
    @GetMapping("/custom/search")
    public ApiResponse<Page<MissionResponse>> searchCustomMissions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) WorryType worryType,
            @RequestParam(required = false) DifficultyLevel difficultyLevel,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionResponse> missions = missionService.searchCustomMissions(
                keyword, worryType, difficultyLevel, pageable, userId);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "커스텀 미션 상세 조회")
    @GetMapping("/custom/{customMissionId}")
    public ApiResponse<MissionResponse> getCustomMission(
            @PathVariable Long customMissionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        MissionResponse mission = missionService.getCustomMission(customMissionId, userId);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 생성", description = "새로운 커스텀 미션을 생성합니다. 로그인 필요.")
    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionResponse> createCustomMission(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid MissionRequest request) {
        MissionResponse mission = missionService.createCustomMission(userId, request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 수정", description = "자신이 만든 커스텀 미션을 수정합니다.")
    @PutMapping("/custom/{customMissionId}")
    public ApiResponse<MissionResponse> updateCustomMission(
            @PathVariable Long customMissionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid MissionRequest request) {
        MissionResponse mission = missionService.updateCustomMission(customMissionId, userId, request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 삭제", description = "자신이 만든 커스텀 미션을 삭제합니다.")
    @DeleteMapping("/custom/{customMissionId}")
    public ApiResponse<Void> deleteCustomMission(
            @PathVariable Long customMissionId,
            @AuthenticationPrincipal Long userId) {
        missionService.deleteCustomMission(customMissionId, userId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "[DEBUG] 사용자 미션 상태 확인", description = "특정 이메일 사용자의 공식 미션 수행/완료 상태를 확인합니다.")
    @GetMapping("/debug/user-status")
    public ApiResponse<Object> getUserMissionStatus(@RequestParam String email) {
        return ApiResponse.success(missionService.getUserMissionStatusForDebug(email));
    }
}
