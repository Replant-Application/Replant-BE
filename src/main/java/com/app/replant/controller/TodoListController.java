package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
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

    @Operation(summary = "투두리스트 초기화",
            description = "새 투두리스트 생성을 위한 랜덤 공식 미션 3개를 조회합니다. (비챌린지 미션만)")
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

    @Operation(summary = "선택 가능한 커스텀 미션 조회",
            description = "투두리스트에 추가할 수 있는 커스텀 미션 목록을 조회합니다. (비챌린지 미션만)")
    @GetMapping("/selectable-missions")
    public ApiResponse<List<TodoListDto.MissionSimpleResponse>> getSelectableMissions(
            @AuthenticationPrincipal Long userId) {
        List<TodoListDto.MissionSimpleResponse> response = todoListService.getSelectableMissions(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "투두리스트 생성",
            description = "랜덤 공식 미션 3개 + 커스텀 미션 2개로 투두리스트를 생성합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "title": "나의 첫 투두리스트",
                      "description": "건강한 습관 만들기",
                      "randomMissionIds": [1, 2, 3],
                      "customMissionIds": [10, 11]
                    }
                    """))
    )
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

    @Operation(summary = "내 투두리스트 목록 조회",
            description = "내 투두리스트 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<Page<TodoListDto.SimpleResponse>> getMyTodoLists(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TodoListDto.SimpleResponse> response = todoListService.getMyTodoLists(userId, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "활성 투두리스트 목록 조회",
            description = "진행중인 투두리스트 목록을 조회합니다.")
    @GetMapping("/active")
    public ApiResponse<List<TodoListDto.SimpleResponse>> getActiveTodoLists(
            @AuthenticationPrincipal Long userId) {
        List<TodoListDto.SimpleResponse> response = todoListService.getActiveTodoLists(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "투두리스트 상세 조회",
            description = "투두리스트의 상세 정보와 포함된 미션 목록을 조회합니다.")
    @GetMapping("/{todoListId}")
    public ApiResponse<TodoListDto.DetailResponse> getTodoListDetail(
            @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
            @AuthenticationPrincipal Long userId) {
        TodoListDto.DetailResponse response = todoListService.getTodoListDetail(todoListId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "투두리스트 미션 완료",
            description = "투두리스트의 특정 미션을 완료 처리합니다.")
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

    @Operation(summary = "새 투두리스트 생성 가능 여부 확인",
            description = "새 투두리스트를 생성할 수 있는지 확인합니다. (활성 투두리스트의 80% 이상 완료 시 가능)")
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

    @Operation(summary = "투두리스트 보관",
            description = "투두리스트를 보관 처리합니다.")
    @PutMapping("/{todoListId}/archive")
    public ApiResponse<Void> archiveTodoList(
            @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
            @AuthenticationPrincipal Long userId) {
        todoListService.archiveTodoList(todoListId, userId);
        return ApiResponse.success(null);
    }

    // ============ 투두 공유 관련 API ============

    @Operation(summary = "공유 가능한 투두리스트 목록",
            description = "투두 공유 탭에서 공유할 수 있는 내 투두리스트 목록을 조회합니다. (비공개 상태인 것만)")
    @GetMapping("/shareable")
    public ApiResponse<List<TodoListDto.SimpleResponse>> getShareableTodoLists(
            @AuthenticationPrincipal Long userId) {
        List<TodoListDto.SimpleResponse> response = todoListService.getShareableTodoLists(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "투두리스트 공유",
            description = "투두리스트를 공개로 전환하여 투두 공유 탭에 공유합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공유 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 공유된 투두리스트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "투두리스트를 찾을 수 없음")
    })
    @PutMapping("/{todoListId}/share")
    public ApiResponse<TodoListDto.DetailResponse> shareTodoList(
            @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
            @AuthenticationPrincipal Long userId) {
        TodoListDto.DetailResponse response = todoListService.shareTodoList(todoListId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "투두리스트 공유 해제",
            description = "공유된 투두리스트를 비공개로 전환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공유 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "투두리스트를 찾을 수 없음")
    })
    @PutMapping("/{todoListId}/unshare")
    public ApiResponse<TodoListDto.DetailResponse> unshareTodoList(
            @Parameter(description = "투두리스트 ID") @PathVariable Long todoListId,
            @AuthenticationPrincipal Long userId) {
        TodoListDto.DetailResponse response = todoListService.unshareTodoList(todoListId, userId);
        return ApiResponse.success(response);
    }
}
