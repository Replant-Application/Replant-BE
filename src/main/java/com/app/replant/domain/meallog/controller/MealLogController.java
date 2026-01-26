package com.app.replant.domain.meallog.controller;

import com.app.replant.domain.meallog.dto.MealLogRequest;
import com.app.replant.domain.meallog.dto.MealLogResponse;
import com.app.replant.domain.meallog.service.MealLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meal-logs")
@RequiredArgsConstructor
@Tag(name = "MealLog", description = "식사 인증 API")
public class MealLogController {

    private final MealLogService mealLogService;

    @Operation(summary = "현재 진행 중인 식사 미션 상태 조회", 
               description = "현재 할당된 식사 미션의 상태와 남은 시간을 조회합니다.")
    @GetMapping("/current")
    public ResponseEntity<MealLogResponse.Status> getCurrentMealMission(
            @AuthenticationPrincipal Long userId) {
        
        MealLogResponse.Status status = mealLogService.getCurrentMealMissionStatus(userId);
        
        if (status == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "식사 인증", 
               description = "게시글과 함께 식사를 인증합니다.")
    @PostMapping("/{mealLogId}/verify")
    public ResponseEntity<MealLogResponse.VerifyResult> verifyMeal(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "식사 기록 ID") @PathVariable Long mealLogId,
            @Valid @RequestBody MealLogRequest.Verify request) {
        
        MealLogResponse.VerifyResult result = mealLogService.verifyMeal(userId, mealLogId, request);
        
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "특정 날짜의 식사 기록 조회", 
               description = "특정 날짜의 아침/점심/저녁 식사 기록을 조회합니다.")
    @GetMapping("/daily")
    public ResponseEntity<MealLogResponse.Daily> getDailyMealLogs(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD), 미입력시 오늘") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) {
            date = LocalDate.now();
        }
        
        MealLogResponse.Daily daily = mealLogService.getDailyMealLogs(userId, date);
        return ResponseEntity.ok(daily);
    }

    @Operation(summary = "날짜 범위의 식사 기록 조회 (캘린더용)", 
               description = "시작일~종료일 사이의 식사 기록을 조회합니다.")
    @GetMapping("/range")
    public ResponseEntity<List<MealLogResponse.Daily>> getMealLogsByRange(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "시작 날짜 (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<MealLogResponse.Daily> mealLogs = mealLogService.getMealLogsByDateRange(userId, startDate, endDate);
        
        return ResponseEntity.ok(mealLogs);
    }

    @Operation(summary = "식사 통계 조회", 
               description = "총 완료 식사 수, 평균 평점, 주간/월간 완료 수를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<MealLogResponse.Stats> getMealStats(
            @AuthenticationPrincipal Long userId) {
        
        MealLogResponse.Stats stats = mealLogService.getMealStats(userId);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "특정 식사 기록 상세 조회")
    @GetMapping("/{mealLogId}")
    public ResponseEntity<MealLogResponse.Detail> getMealLogDetail(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "식사 기록 ID") @PathVariable Long mealLogId) {
        
        MealLogResponse.Detail detail = mealLogService.getMealLogDetail(userId, mealLogId);
        
        return ResponseEntity.ok(detail);
    }
}
