package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.mission.dto.MissionRequest;
import com.app.replant.domain.mission.dto.MissionResponse;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "CustomMission", description = "커스텀 미션 API - 사용자가 직접 생성하는 미션")
@RestController
@RequestMapping("/api/missions/custom")
@RequiredArgsConstructor
public class CustomMissionController {

    private final MissionService missionService;

    @Operation(summary = "커스텀 미션 목록 조회",
            description = "공개된 커스텀 미션 목록을 조회합니다. 인증 타입으로 필터링 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "content": [
                                  {
                                    "id": 1,
                                    "title": "매일 물 2L 마시기",
                                    "description": "건강을 위해 하루 물 2L를 마셔보세요",
                                    "creatorId": 10,
                                    "creatorNickname": "건강지킴이",
                                    "worryType": "SELF_MANAGEMENT",
                                    "missionType": "DAILY",
                                    "difficultyLevel": "EASY",
                                    "durationDays": 7,
                                    "isPublic": true,
                                    "verificationType": "COMMUNITY",
                                    "expReward": 10,
                                    "badgeDurationDays": 3,
                                    "isActive": true,
                                    "createdAt": "2024-01-15T10:30:00"
                                  }
                                ],
                                "totalElements": 1,
                                "totalPages": 1,
                                "number": 0
                              }
                            }
                            """)))
    })
    @GetMapping
    public ApiResponse<Page<MissionResponse>> getCustomMissions(
            @Parameter(description = "인증 타입 필터 (COMMUNITY, GPS, TIME)")
            @RequestParam(required = false) VerificationType verificationType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionResponse> missions = missionService.getCustomMissions(verificationType, pageable);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "커스텀 미션 상세 조회",
            description = "커스텀 미션의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "미션을 찾을 수 없음")
    })
    @GetMapping("/{customMissionId}")
    public ApiResponse<MissionResponse> getCustomMission(
            @Parameter(description = "커스텀 미션 ID", example = "1")
            @PathVariable Long customMissionId) {
        MissionResponse mission = missionService.getCustomMission(customMissionId);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 생성",
            description = "새로운 커스텀 미션을 생성합니다. 로그인 필요.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "title": "매일 아침 스트레칭 10분",
                      "description": "하루를 상쾌하게 시작하기 위한 아침 스트레칭",
                      "worryType": "SELF_MANAGEMENT",
                      "missionType": "DAILY",
                      "difficultyLevel": "EASY",
                      "durationDays": 7,
                      "isPublic": true,
                      "verificationType": "COMMUNITY",
                      "expReward": 15,
                      "badgeDurationDays": 3
                    }
                    """))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionResponse> createCustomMission(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid MissionRequest request) {
        MissionResponse mission = missionService.createCustomMission(userId, request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 수정",
            description = "자신이 생성한 커스텀 미션을 수정합니다. 생성자만 수정 가능.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "title": "매일 아침 스트레칭 15분",
                      "description": "하루를 상쾌하게 시작하기 위한 아침 스트레칭 (수정됨)",
                      "worryType": "SELF_MANAGEMENT",
                      "missionType": "DAILY",
                      "difficultyLevel": "MEDIUM",
                      "expReward": 20,
                      "isPublic": true
                    }
                    """))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (생성자가 아님)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "미션을 찾을 수 없음")
    })
    @PutMapping("/{customMissionId}")
    public ApiResponse<MissionResponse> updateCustomMission(
            @Parameter(description = "커스텀 미션 ID", example = "1")
            @PathVariable Long customMissionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid MissionRequest request) {
        MissionResponse mission = missionService.updateCustomMission(customMissionId, userId, request);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "커스텀 미션 삭제",
            description = "자신이 생성한 커스텀 미션을 삭제합니다. 생성자만 삭제 가능.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "message": "커스텀 미션이 삭제되었습니다."
                              }
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "미션을 찾을 수 없음")
    })
    @DeleteMapping("/{customMissionId}")
    public ApiResponse<Map<String, String>> deleteCustomMission(
            @Parameter(description = "커스텀 미션 ID", example = "1")
            @PathVariable Long customMissionId,
            @AuthenticationPrincipal Long userId) {
        missionService.deleteCustomMission(customMissionId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "커스텀 미션이 삭제되었습니다.");

        return ApiResponse.success(result);
    }
}
