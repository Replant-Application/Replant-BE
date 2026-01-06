package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.diary.dto.DiaryRequest;
import com.app.replant.domain.diary.dto.DiaryResponse;
import com.app.replant.domain.diary.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Diary", description = "다이어리 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT Token")
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "다이어리 목록 조회")
    @GetMapping
    public ApiResponse<Page<DiaryResponse>> getDiaries(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DiaryResponse> diaries = diaryService.getDiaries(userId, pageable);
        return ApiResponse.success(diaries);
    }

    @Operation(summary = "다이어리 상세 조회")
    @GetMapping("/{diaryId}")
    public ApiResponse<DiaryResponse> getDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal Long userId) {
        DiaryResponse diary = diaryService.getDiary(diaryId, userId);
        return ApiResponse.success(diary);
    }

    @Operation(summary = "날짜별 다이어리 조회")
    @GetMapping("/by-date")
    public ApiResponse<DiaryResponse> getDiaryByDate(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DiaryResponse diary = diaryService.getDiaryByDate(userId, date);
        return ApiResponse.success(diary);
    }

    @Operation(summary = "기간별 다이어리 조회")
    @GetMapping("/range")
    public ApiResponse<List<DiaryResponse>> getDiariesByDateRange(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DiaryResponse> diaries = diaryService.getDiariesByDateRange(userId, startDate, endDate);
        return ApiResponse.success(diaries);
    }

    @Operation(summary = "다이어리 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DiaryResponse> createDiary(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid DiaryRequest request) {
        DiaryResponse diary = diaryService.createDiary(userId, request);
        return ApiResponse.success(diary);
    }

    @Operation(summary = "다이어리 수정")
    @PutMapping("/{diaryId}")
    public ApiResponse<DiaryResponse> updateDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid DiaryRequest request) {
        DiaryResponse diary = diaryService.updateDiary(diaryId, userId, request);
        return ApiResponse.success(diary);
    }

    @Operation(summary = "다이어리 삭제")
    @DeleteMapping("/{diaryId}")
    public ApiResponse<Map<String, String>> deleteDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal Long userId) {
        diaryService.deleteDiary(diaryId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "다이어리가 삭제되었습니다.");
        return ApiResponse.success(result);
    }

    @Operation(summary = "다이어리 통계 조회")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getDiaryStats(
            @AuthenticationPrincipal Long userId) {
        Map<String, Object> stats = diaryService.getDiaryStats(userId);
        return ApiResponse.success(stats);
    }
}
