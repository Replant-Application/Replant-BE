package com.app.replant.domain.missionset.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.missionset.dto.TodoListDto;
import com.app.replant.domain.missionset.service.TodoListService;
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
import java.util.List;
import java.util.Map;

@Tag(name = "TodoList", description = "투두리스트 API")
@RestController
@RequestMapping("/api/todolists")
@RequiredArgsConstructor
public class TodoListController {

        private final TodoListService todoListService;

        @Operation(summary = "투두리스트 초기화", description = "새 투두리스트 생성을 위한 랜덤 공식 미션 3개를 조회합니다. (비챌린지 미션만)")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @PostMapping("/init")
        public ApiResponse<TodoListDto.InitResponse> initTodoList(
                        @AuthenticationPrincipal Long userId) {
                TodoListDto.InitResponse response = todoListService.initTodoList(userId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "선택 가능한 커스텀 미션 조회", description = "투두리스트에 추가할 수 있는 커스텀 미션 목록을 조회합니다. (비챌린지 미션만)")
        @GetMapping("/selectable-missions")
        public ApiResponse<List<TodoListDto.MissionSimpleResponse>> getSelectableMissions(
                        @AuthenticationPrincipal Long userId) {
                List<TodoListDto.MissionSimpleResponse> response = todoListService.getSelectableMissions(userId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "랜덤 미션 리롤", description = "기존 미션을 제외하고 새로운 랜덤 공식 미션 1개를 조회합니다. (투두리스트 생성 시 미션 교체용)")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
                        {
                          "excludeMissionIds": [1, 2, 3]
                        }
                        """)))
        @PostMapping("/reroll-mission")
        public ApiResponse<TodoListDto.MissionSimpleResponse> rerollRandomMission(
                        @AuthenticationPrincipal Long userId,
                        @RequestBody Map<String, List<Long>> request) {
                List<Long> excludeMissionIds = request.getOrDefault("excludeMissionIds", java.util.Collections.emptyList());
                TodoListDto.MissionSimpleResponse response = todoListService.rerollRandomMission(userId, excludeMissionIds);
                return ApiResponse.success(response);
        }

        @Operation(summary = "투두리스트 생성", description = "필수 공식 미션 3개 + 선택 커스텀 미션(0개 이상)으로 투두리스트를 생성합니다. 각 미션에 시간대를 설정할 수 있습니다.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
                        {
                          "title": "나의 첫 투두리스트",
                          "description": "건강한 습관 만들기",
                          "randomMissionIds": [1, 2, 3],
                          "customMissionIds": [10, 11],
                          "missionSchedules": {
                            "1": { "startTime": "09:00", "endTime": "10:00" },
                            "2": { "startTime": "10:00", "endTime": "11:00" },
                            "3": { "startTime": "11:00", "endTime": "12:00" },
                            "10": { "startTime": "14:00", "endTime": "15:00" },
                            "11": { "startTime": "15:00", "endTime": "16:00" }
                          }
                        }
                        """)))
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (미션 개수 부족 등)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public ApiResponse<TodoListDto.DetailResponse> createTodoList(
                        @AuthenticationPrincipal Long userId,
                        @RequestBody TodoListDto.CreateRequest request) {
                TodoListDto.DetailResponse response = todoListService.createTodoList(userId, request);
                return ApiResponse.success(response);
        }

        @Operation(summary = "내 투두리스트 목록 조회", description = "내 투두리스트 목록을 조회합니다.")
        @GetMapping
        public ApiResponse<Page<TodoListDto.SimpleResponse>> getMyTodoLists(
                        @AuthenticationPrincipal Long userId,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                Page<TodoListDto.SimpleResponse> response = todoListService.getMyTodoLists(userId, pageable);
                return ApiResponse.success(response);
        }

        @Operation(summary = "활성 투두리스트 목록 조회", description = "진행중인 투두리스트 목록을 조회합니다.")
        @GetMapping("/active")
        public ApiResponse<List<TodoListDto.SimpleResponse>> getActiveTodoLists(
                        @AuthenticationPrincipal Long userId) {
                List<TodoListDto.SimpleResponse> response = todoListService.getActiveTodoLists(userId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "투두리스트 상세 조회", description = "투두리스트의 상세 정보와 포함된 미션 목록을 조회합니다.")
        @GetMapping("/{todoListId}")
        public ApiResponse<TodoListDto.DetailResponse> getTodoListDetail(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @AuthenticationPrincipal Long userId) {
                TodoListDto.DetailResponse response = todoListService.getTodoListDetail(todoListId, userId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "투두리스트 미션 완료", description = "투두리스트의 특정 미션을 완료 처리합니다.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 완료된 미션"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "투두리스트 또는 미션을 찾을 수 없음")
        })
        @PutMapping("/{todoListId}/missions/{missionId}/complete")
        public ApiResponse<TodoListDto.DetailResponse> completeMission(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @Parameter(description = "미션 ID") @PathVariable Long missionId,
                        @AuthenticationPrincipal Long userId) {
                TodoListDto.DetailResponse response = todoListService.completeMission(todoListId, missionId, userId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "새 투두리스트 생성 가능 여부 확인", description = "새 투두리스트를 생성할 수 있는지 확인합니다. (활성 투두리스트의 80% 이상 완료 시 가능)")
        @GetMapping("/can-create")
        public ApiResponse<Map<String, Object>> canCreateNewTodoList(
                        @AuthenticationPrincipal Long userId) {
                boolean canCreate = todoListService.canCreateNewTodoList(userId);
                long activeCount = todoListService.getActiveTodoListCount(userId);

                Map<String, Object> response = new HashMap<>();
                response.put("canCreate", canCreate);
                response.put("activeTodoListCount", activeCount);
                return ApiResponse.success(response);
        }

        @Operation(summary = "투두리스트 보관", description = "투두리스트를 보관 처리합니다.")
        @PutMapping("/{todoListId}/archive")
        public ApiResponse<Void> archiveTodoList(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @AuthenticationPrincipal Long userId) {
                todoListService.archiveTodoList(todoListId, userId);
                return ApiResponse.success(null);
        }


        @Operation(summary = "투두리스트 리뷰 목록 조회", description = "투두리스트의 리뷰 목록을 조회합니다.")
        @GetMapping("/{todoListId}/reviews")
        public ApiResponse<Page<TodoListDto.ReviewResponse>> getReviews(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                Page<TodoListDto.ReviewResponse> response = todoListService.getReviews(todoListId, pageable);
                return ApiResponse.success(response);
        }

        @Operation(summary = "내 리뷰 조회", description = "특정 투두리스트에 작성한 내 리뷰를 조회합니다.")
        @GetMapping("/{todoListId}/reviews/my")
        public ApiResponse<TodoListDto.ReviewResponse> getMyReview(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @AuthenticationPrincipal Long userId) {
                TodoListDto.ReviewResponse response = todoListService.getMyReview(todoListId, userId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "리뷰 수정", description = "작성한 리뷰를 수정합니다.")
        @PutMapping("/reviews/{reviewId}")
        public ApiResponse<TodoListDto.ReviewResponse> updateReview(
                        @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
                        @AuthenticationPrincipal Long userId,
                        @RequestBody TodoListDto.UpdateReviewRequest request) {
                TodoListDto.ReviewResponse response = todoListService.updateReview(reviewId, userId, request);
                return ApiResponse.success(response);
        }

        @Operation(summary = "리뷰 삭제", description = "작성한 리뷰를 삭제합니다.")
        @DeleteMapping("/reviews/{reviewId}")
        public ApiResponse<Void> deleteReview(
                        @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
                        @AuthenticationPrincipal Long userId) {
                todoListService.deleteReview(reviewId, userId);
                return ApiResponse.success(null);
        }

        // ============ 투두리스트 CRUD API ============

        @Operation(summary = "투두리스트 수정", description = "투두리스트의 제목, 설명, 공개 여부를 수정합니다.")
        @PutMapping("/{todoListId}")
        public ApiResponse<TodoListDto.DetailResponse> updateTodoList(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @AuthenticationPrincipal Long userId,
                        @RequestBody TodoListDto.UpdateRequest request) {
                TodoListDto.DetailResponse response = todoListService.updateTodoList(todoListId, userId, request);
                return ApiResponse.success(response);
        }

        @Operation(summary = "투두리스트 삭제", description = "투두리스트를 삭제합니다. (Soft Delete)")
        @DeleteMapping("/{todoListId}")
        public ApiResponse<Void> deleteTodoList(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @AuthenticationPrincipal Long userId) {
                todoListService.deleteTodoList(todoListId, userId);
                return ApiResponse.success(null);
        }

        @Operation(summary = "투두리스트에 미션 추가", description = "투두리스트에 새로운 미션을 추가합니다.")
        @PostMapping("/{todoListId}/missions")
        @ResponseStatus(HttpStatus.CREATED)
        public ApiResponse<TodoListDto.DetailResponse> addMissionToTodoList(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @AuthenticationPrincipal Long userId,
                        @RequestBody TodoListDto.AddMissionRequest request) {
                TodoListDto.DetailResponse response = todoListService.addMissionToTodoList(todoListId, userId, request);
                return ApiResponse.success(response);
        }

        @Operation(summary = "투두리스트에서 미션 제거", description = "투두리스트에서 특정 미션을 제거합니다.")
        @DeleteMapping("/{todoListId}/missions/{missionId}")
        public ApiResponse<TodoListDto.DetailResponse> removeMissionFromTodoList(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @Parameter(description = "미션 ID") @PathVariable Long missionId,
                        @AuthenticationPrincipal Long userId) {
                TodoListDto.DetailResponse response = todoListService.removeMissionFromTodoList(todoListId, missionId,
                                userId);
                return ApiResponse.success(response);
        }

        @Operation(summary = "투두리스트 미션 순서 변경", description = "투두리스트 내 미션들의 순서를 변경합니다.")
        @PutMapping("/{todoListId}/missions/reorder")
        public ApiResponse<TodoListDto.DetailResponse> reorderMissions(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @AuthenticationPrincipal Long userId,
                        @RequestBody TodoListDto.ReorderMissionsRequest request) {
                TodoListDto.DetailResponse response = todoListService.reorderMissions(todoListId, userId, request);
                return ApiResponse.success(response);
        }

        @Operation(summary = "투두리스트 미션 시간대 설정", description = "투두리스트 내 특정 미션의 시간대를 설정합니다.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
                        {
                          "startTime": "09:00",
                          "endTime": "10:00"
                        }
                        """)))
        @PatchMapping("/{todoListId}/missions/{missionId}/schedule")
        public ApiResponse<TodoListDto.DetailResponse> updateMissionSchedule(
                        @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
                        @Parameter(description = "미션 ID") @PathVariable Long missionId,
                        @AuthenticationPrincipal Long userId,
                        @RequestBody TodoListDto.UpdateMissionScheduleRequest request) {
                TodoListDto.DetailResponse response = todoListService.updateMissionSchedule(todoListId, missionId, userId,
                                request);
                return ApiResponse.success(response);
        }
}
