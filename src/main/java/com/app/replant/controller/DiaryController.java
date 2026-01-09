package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.common.dto.PageResponse;
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
import java.util.Map;

@Tag(name = "Diary", description = "다이어리 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT Token")
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(
            summary = "다이어리 목록 조회",
            description = "사용자가 작성한 감정일기 목록을 조회합니다. 날짜순으로 정렬되어 최신 일기가 먼저 표시됩니다. " +
                    "페이징 파라미터: page(페이지 번호, 0부터 시작), size(페이지 크기, 기본값 20), sort(정렬 필드, 예: date,desc)")
    @GetMapping
    public ApiResponse<PageResponse<DiaryResponse>> getDiaries(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DiaryResponse> diaries = diaryService.getDiaries(userId, pageable);
        return ApiResponse.success(PageResponse.from(diaries));
    }

    @Operation(
            summary = "날짜별 다이어리 조회",
            description = "특정 날짜에 작성한 다이어리를 조회합니다. 날짜 형식: YYYY-MM-DD (예: 2026-01-09)"
    )
    @GetMapping("/by-date")
    public ApiResponse<DiaryResponse> getDiaryByDate(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DiaryResponse diary = diaryService.getDiaryByDate(userId, date);
        return ApiResponse.success(diary);
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

}
