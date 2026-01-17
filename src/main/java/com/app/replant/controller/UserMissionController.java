package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.usermission.dto.*;
import com.app.replant.domain.usermission.service.UserMissionService;
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

@Tag(name = "UserMission", description = "내 미션 API - 사용자에게 할당된 미션 관리")
@RestController
@RequestMapping("/api/missions/my")
@RequiredArgsConstructor
public class UserMissionController {

        private final UserMissionService userMissionService;

        @Operation(summary = "내 미션 목록 조회", description = "로그인한 사용자의 미션 목록을 조회합니다. 상태 및 미션 타입으로 필터링 가능합니다.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(examples = @ExampleObject(value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "content": [
                                              {
                                                "id": 1,
                                                "missionType": "SYSTEM",
                                                "mission": {
                                                  "id": 10,
                                                  "title": "매일 물 2L 마시기",
                                                  "description": "건강을 위해 물을 충분히 마셔보세요"
                                                },
                                                "status": "ASSIGNED",
                                                "assignedAt": "2024-01-15T09:00:00",
                                                "dueDate": "2024-01-22T23:59:59"
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
        public ApiResponse<Page<UserMissionResponse>> getUserMissions(
                        @AuthenticationPrincipal Long userId,
                        @PageableDefault(size = 20, sort = "assignedAt", direction = Sort.Direction.DESC) Pageable pageable) {
                // 정렬 필드 검증: 허용된 필드만 사용 (id, assignedAt, dueDate, createdAt, status)
                Pageable validatedPageable = pageable;
                if (pageable.getSort().isSorted()) {
                        String sortProperty = pageable.getSort().stream()
                                        .findFirst()
                                        .map(Sort.Order::getProperty)
                                        .orElse("assignedAt");
                        
                        // 허용된 정렬 필드 목록
                        if (!sortProperty.equals("id") && 
                            !sortProperty.equals("assignedAt") && 
                            !sortProperty.equals("dueDate") && 
                            !sortProperty.equals("createdAt") && 
                            !sortProperty.equals("status")) {
                                // 잘못된 필드면 기본값으로 대체
                                validatedPageable = org.springframework.data.domain.PageRequest.of(
                                        pageable.getPageNumber(),
                                        pageable.getPageSize(),
                                        Sort.by(Sort.Direction.DESC, "assignedAt")
                                );
                        }
                }
                
                Page<UserMissionResponse> missions = userMissionService.getUserMissions(userId, validatedPageable);
                return ApiResponse.success(missions);
        }

        @Operation(summary = "내 미션 상세 조회", description = "특정 미션의 상세 정보를 조회합니다.")
        @GetMapping("/{userMissionId}")
        public ApiResponse<UserMissionResponse> getUserMission(
                        @Parameter(description = "사용자 미션 ID", example = "1") @PathVariable Long userMissionId,
                        @AuthenticationPrincipal Long userId) {
                UserMissionResponse mission = userMissionService.getUserMission(userMissionId, userId);
                return ApiResponse.success(mission);
        }

        @Operation(summary = "커스텀 미션을 내 미션에 추가", description = "공개된 커스텀 미션을 내 미션 목록에 추가합니다.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
                        {
                          "customMissionId": 5
                        }
                        """)))
        @PostMapping("/custom")
        @ResponseStatus(HttpStatus.CREATED)
        public ApiResponse<UserMissionResponse> addCustomMission(
                        @AuthenticationPrincipal Long userId,
                        @RequestBody @Valid AddCustomMissionRequest request) {
                UserMissionResponse mission = userMissionService.addCustomMission(userId, request);
                return ApiResponse.success(mission);
        }

        @Operation(summary = "시스템 미션을 내 미션에 추가", description = "시스템 미션을 내 미션 목록에 추가합니다.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
                        {
                          "missionId": 10
                        }
                        """)))
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public ApiResponse<UserMissionResponse> addMission(
                        @AuthenticationPrincipal Long userId,
                        @RequestBody @Valid AddMissionRequest request) {
                UserMissionResponse mission = userMissionService.addMission(userId, request);
                return ApiResponse.success(mission);
        }

        @GetMapping("/history")
        @Operation(summary = "미션 완료 이력 조회", description = "완료한 미션의 이력을 조회합니다.")
        public ApiResponse<Page<UserMissionResponse>> getMissionHistory(
                        @AuthenticationPrincipal Long userId,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                Page<UserMissionResponse> history = userMissionService.getMissionHistory(userId, pageable);
                return ApiResponse.success(history);
        }

        @Operation(summary = "돌발 미션 인증", description = "돌발 미션을 인증합니다. 기상 미션은 시간 제한(10분), 식사 미션은 게시글 작성으로 인증합니다.")
        @PostMapping("/{userMissionId}/verify-spontaneous")
        public ApiResponse<VerifyMissionResponse> verifySpontaneousMission(
                        @Parameter(description = "사용자 미션 ID", example = "1") @PathVariable Long userMissionId,
                        @AuthenticationPrincipal Long userId,
                        @RequestBody @Valid VerifySpontaneousMissionRequest request) {
                VerifyMissionResponse response = userMissionService.verifySpontaneousMission(userMissionId, userId, request);
                return ApiResponse.success(response);
        }

        @Operation(summary = "기상 미션 상태 조회", description = "현재 사용자의 활성화된 기상 미션 상태를 조회합니다.")
        @GetMapping("/wakeup/current")
        public ApiResponse<WakeUpMissionStatusResponse> getCurrentWakeUpMission(
                        @AuthenticationPrincipal Long userId) {
                WakeUpMissionStatusResponse status = userMissionService.getCurrentWakeUpMissionStatus(userId);
                if (status == null) {
                        throw new com.app.replant.exception.CustomException(
                                        com.app.replant.exception.ErrorCode.USER_MISSION_NOT_FOUND,
                                        "현재 활성화된 기상 미션이 없습니다.");
                }
                return ApiResponse.success(status);
        }

        @Operation(summary = "기상 미션 인증 (간편)", description = "기상 미션을 간편하게 인증합니다. userMissionId를 쿼리 파라미터로 받습니다. userMissionId가 없으면 자동으로 찾습니다. GET/POST 모두 지원 (POST 권장).")
        @RequestMapping(value = "/wakeup/verify-time", method = {RequestMethod.GET, RequestMethod.POST})
        public ApiResponse<VerifyMissionResponse> verifyWakeUpMissionWithQuery(
                        @Parameter(description = "사용자 미션 ID (선택사항, 없으면 자동으로 찾음)", example = "1") @RequestParam(required = false) Long userMissionId,
                        @AuthenticationPrincipal Long userId) {
                // userMissionId가 없으면 현재 사용자의 ASSIGNED 상태인 기상 미션을 자동으로 찾기
                if (userMissionId == null) {
                        userMissionId = userMissionService.findCurrentWakeUpMissionId(userId);
                        if (userMissionId == null) {
                                throw new com.app.replant.exception.CustomException(
                                                com.app.replant.exception.ErrorCode.USER_MISSION_NOT_FOUND,
                                                "인증할 기상 미션을 찾을 수 없습니다.");
                        }
                }
                
                // 기상 미션 인증 요청 생성 (postId 없음)
                VerifySpontaneousMissionRequest request = new VerifySpontaneousMissionRequest();
                VerifyMissionResponse response = userMissionService.verifySpontaneousMission(userMissionId, userId, request);
                return ApiResponse.success(response);
        }
        
        @Operation(summary = "기상 미션 인증 (경로 변수)", description = "기상 미션을 경로 변수로 인증합니다. userMissionId를 URL 경로에 포함합니다.")
        @RequestMapping(value = "/wakeup/{userMissionId}/verify-time", method = {RequestMethod.GET, RequestMethod.POST})
        public ApiResponse<VerifyMissionResponse> verifyWakeUpMissionWithPath(
                        @Parameter(description = "사용자 미션 ID", example = "1") @PathVariable Long userMissionId,
                        @AuthenticationPrincipal Long userId) {
                // 기상 미션 인증 요청 생성 (postId 없음)
                VerifySpontaneousMissionRequest request = new VerifySpontaneousMissionRequest();
                VerifyMissionResponse response = userMissionService.verifySpontaneousMission(userMissionId, userId, request);
                return ApiResponse.success(response);
        }
}
