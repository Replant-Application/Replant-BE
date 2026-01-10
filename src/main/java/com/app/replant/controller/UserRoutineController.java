package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.routine.dto.UserRoutineRequest;
import com.app.replant.domain.routine.dto.UserRoutineResponse;
import com.app.replant.domain.routine.enums.PeriodType;
import com.app.replant.domain.routine.enums.RoutineType;
import com.app.replant.domain.routine.service.UserRoutineService;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "UserRoutine", description = "나의 루틴 설정 API")
@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
public class UserRoutineController {

    private final UserRoutineService routineService;

    @Operation(summary = "루틴 타입 목록 조회", description = "설정 가능한 모든 루틴 타입 목록을 조회합니다.")
    @GetMapping("/types")
    public ApiResponse<List<Map<String, Object>>> getRoutineTypes() {
        List<Map<String, Object>> types = Arrays.stream(RoutineType.values())
                .map(type -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", type);
                    map.put("displayName", type.getDisplayName());
                    map.put("defaultPeriodType", type.getDefaultPeriodType());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(types);
    }

    @Operation(summary = "활성 루틴 전체 조회", description = "현재 활성화된 모든 루틴 설정을 조회합니다.")
    @GetMapping
    public ApiResponse<List<UserRoutineResponse>> getActiveRoutines(
            @AuthenticationPrincipal Long userId) {
        List<UserRoutineResponse> routines = routineService.getActiveRoutines(userId);
        return ApiResponse.success(routines);
    }

    @Operation(summary = "주기별 활성 루틴 조회", description = "특정 주기(매일/매주/매월)의 활성 루틴을 조회합니다.")
    @GetMapping("/period/{periodType}")
    public ApiResponse<List<UserRoutineResponse>> getRoutinesByPeriod(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "주기 타입 (DAILY, WEEKLY, MONTHLY)")
            @PathVariable PeriodType periodType) {
        List<UserRoutineResponse> routines = routineService.getActiveRoutinesByPeriod(userId, periodType);
        return ApiResponse.success(routines);
    }

    @Operation(summary = "특정 타입 활성 루틴 조회", description = "특정 루틴 타입의 현재 활성 설정을 조회합니다.")
    @GetMapping("/type/{routineType}")
    public ApiResponse<UserRoutineResponse> getRoutineByType(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "루틴 타입")
            @PathVariable RoutineType routineType) {
        UserRoutineResponse routine = routineService.getActiveRoutine(userId, routineType);
        return ApiResponse.success(routine);
    }

    @Operation(summary = "루틴 히스토리 조회", description = "특정 루틴 타입의 과거 기록을 조회합니다.")
    @GetMapping("/type/{routineType}/history")
    public ApiResponse<Page<UserRoutineResponse>> getRoutineHistory(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "루틴 타입")
            @PathVariable RoutineType routineType,
            @PageableDefault(size = 10, sort = "periodStart", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserRoutineResponse> history = routineService.getRoutineHistory(userId, routineType, pageable);
        return ApiResponse.success(history);
    }

    @Operation(summary = "루틴 설정 저장", description = "새 루틴을 생성하거나 기존 루틴을 업데이트합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserRoutineResponse> saveRoutine(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UserRoutineRequest request) {
        UserRoutineResponse routine = routineService.saveRoutine(userId, request);
        return ApiResponse.success(routine);
    }

    @Operation(summary = "루틴 삭제", description = "루틴 설정을 비활성화합니다.")
    @DeleteMapping("/{routineId}")
    public ApiResponse<Map<String, String>> deleteRoutine(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long routineId) {
        routineService.deleteRoutine(userId, routineId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "루틴이 삭제되었습니다.");
        return ApiResponse.success(result);
    }

    @Operation(summary = "루틴 알림 토글", description = "루틴의 알림 설정을 켜거나 끕니다.")
    @PatchMapping("/{routineId}/notification")
    public ApiResponse<UserRoutineResponse> toggleNotification(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long routineId,
            @RequestParam Boolean enabled) {
        UserRoutineResponse routine = routineService.toggleNotification(userId, routineId, enabled);
        return ApiResponse.success(routine);
    }
}
