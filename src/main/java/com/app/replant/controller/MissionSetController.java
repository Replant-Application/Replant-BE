package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.missionset.dto.MissionSetDto;
import com.app.replant.domain.missionset.service.MissionSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "MissionSet", description = "미션세트(투두리스트) API")
@RestController
@RequestMapping("/api/mission-sets")
@RequiredArgsConstructor
public class MissionSetController {

    private final MissionSetService missionSetService;

    @Operation(summary = "미션세트 생성",
            description = "새로운 미션세트(투두리스트)를 생성합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "title": "아침 루틴",
                      "description": "건강한 아침 습관 만들기",
                      "isPublic": true,
                      "missionIds": [1, 2, 3]
                    }
                    """))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionSetDto.DetailResponse> createMissionSet(
            @AuthenticationPrincipal Long userId,
            @RequestBody MissionSetDto.CreateRequest request) {
        MissionSetDto.DetailResponse response = missionSetService.createMissionSet(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "내 미션세트 목록 조회",
            description = "내가 만든 미션세트 목록을 조회합니다.")
    @GetMapping("/my")
    public ApiResponse<Page<MissionSetDto.SimpleResponse>> getMyMissionSets(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionSetDto.SimpleResponse> response = missionSetService.getMyMissionSets(userId, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "공개 미션세트 목록 조회",
            description = "공개된 미션세트 목록을 담은수 + 평점 순으로 조회합니다.")
    @GetMapping
    public ApiResponse<Page<MissionSetDto.SimpleResponse>> getPublicMissionSets(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MissionSetDto.SimpleResponse> response = missionSetService.getPublicMissionSets(pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "공개 미션세트 검색",
            description = "키워드로 공개 미션세트를 검색합니다.")
    @GetMapping("/search")
    public ApiResponse<Page<MissionSetDto.SimpleResponse>> searchPublicMissionSets(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MissionSetDto.SimpleResponse> response = missionSetService.searchPublicMissionSets(keyword, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "미션세트 상세 조회",
            description = "미션세트의 상세 정보와 포함된 미션 목록을 조회합니다.")
    @GetMapping("/{missionSetId}")
    public ApiResponse<MissionSetDto.DetailResponse> getMissionSetDetail(
            @Parameter(description = "미션세트 ID") @PathVariable Long missionSetId,
            @AuthenticationPrincipal Long userId) {
        MissionSetDto.DetailResponse response = missionSetService.getMissionSetDetail(missionSetId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "미션세트 수정",
            description = "미션세트 정보를 수정합니다. 생성자만 가능합니다.")
    @PutMapping("/{missionSetId}")
    public ApiResponse<MissionSetDto.DetailResponse> updateMissionSet(
            @Parameter(description = "미션세트 ID") @PathVariable Long missionSetId,
            @AuthenticationPrincipal Long userId,
            @RequestBody MissionSetDto.UpdateRequest request) {
        MissionSetDto.DetailResponse response = missionSetService.updateMissionSet(missionSetId, userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "미션세트 삭제",
            description = "미션세트를 삭제합니다. 생성자만 가능합니다.")
    @DeleteMapping("/{missionSetId}")
    public ApiResponse<Map<String, String>> deleteMissionSet(
            @Parameter(description = "미션세트 ID") @PathVariable Long missionSetId,
            @AuthenticationPrincipal Long userId) {
        missionSetService.deleteMissionSet(missionSetId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "미션세트가 삭제되었습니다.");
        return ApiResponse.success(result);
    }

    @Operation(summary = "미션세트에 미션 추가",
            description = "미션세트에 미션을 추가합니다. 생성자만 가능합니다.")
    @PostMapping("/{missionSetId}/missions")
    public ApiResponse<MissionSetDto.DetailResponse> addMissionToSet(
            @Parameter(description = "미션세트 ID") @PathVariable Long missionSetId,
            @AuthenticationPrincipal Long userId,
            @RequestBody MissionSetDto.AddMissionRequest request) {
        MissionSetDto.DetailResponse response = missionSetService.addMissionToSet(missionSetId, userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "미션세트에서 미션 제거",
            description = "미션세트에서 미션을 제거합니다. 생성자만 가능합니다.")
    @DeleteMapping("/{missionSetId}/missions/{missionId}")
    public ApiResponse<MissionSetDto.DetailResponse> removeMissionFromSet(
            @Parameter(description = "미션세트 ID") @PathVariable Long missionSetId,
            @Parameter(description = "미션 ID") @PathVariable Long missionId,
            @AuthenticationPrincipal Long userId) {
        MissionSetDto.DetailResponse response = missionSetService.removeMissionFromSet(missionSetId, missionId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "미션세트 미션 순서 변경",
            description = "미션세트에 포함된 미션들의 순서를 변경합니다. 생성자만 가능합니다.")
    @PutMapping("/{missionSetId}/missions/reorder")
    public ApiResponse<MissionSetDto.DetailResponse> reorderMissions(
            @Parameter(description = "미션세트 ID") @PathVariable Long missionSetId,
            @AuthenticationPrincipal Long userId,
            @RequestBody MissionSetDto.ReorderMissionsRequest request) {
        MissionSetDto.DetailResponse response = missionSetService.reorderMissions(missionSetId, userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "미션세트 담기",
            description = "다른 사용자의 공개 미션세트를 내 미션세트로 복사합니다.")
    @PostMapping("/{missionSetId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionSetDto.DetailResponse> copyMissionSet(
            @Parameter(description = "미션세트 ID") @PathVariable Long missionSetId,
            @AuthenticationPrincipal Long userId) {
        MissionSetDto.DetailResponse response = missionSetService.copyMissionSet(missionSetId, userId);
        return ApiResponse.success(response);
    }
}
