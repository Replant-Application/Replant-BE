package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.mission.dto.*;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Mission", description = "시스템 미션 API")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @Operation(summary = "미션 목록 조회")
    @GetMapping
    public ApiResponse<Page<MissionResponse>> getMissions(
            @RequestParam(required = false) MissionType type,
            @RequestParam(required = false) VerificationType verificationType,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MissionResponse> missions = missionService.getMissions(type, verificationType, pageable);
        return ApiResponse.success(missions);
    }

    @Operation(summary = "미션 상세 조회")
    @GetMapping("/{missionId}")
    public ApiResponse<MissionResponse> getMission(@PathVariable Long missionId) {
        MissionResponse mission = missionService.getMission(missionId);
        return ApiResponse.success(mission);
    }

    @Operation(summary = "미션 리뷰 목록 조회")
    @GetMapping("/{missionId}/reviews")
    public ApiResponse<Page<MissionReviewResponse>> getReviews(
            @PathVariable Long missionId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MissionReviewResponse> reviews = missionService.getReviews(missionId, pageable);
        return ApiResponse.success(reviews);
    }

    @Operation(summary = "미션 리뷰 작성")
    @PostMapping("/{missionId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionReviewResponse> createReview(
            @PathVariable Long missionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid MissionReviewRequest request) {
        MissionReviewResponse review = missionService.createReview(missionId, userId, request);
        return ApiResponse.success(review);
    }

    @Operation(summary = "미션 QnA 목록 조회")
    @GetMapping("/{missionId}/qna")
    public ApiResponse<Page<MissionQnAResponse>> getQnAList(
            @PathVariable Long missionId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MissionQnAResponse> qnaList = missionService.getQnAList(missionId, pageable);
        return ApiResponse.success(qnaList);
    }

    @Operation(summary = "미션 QnA 상세 조회")
    @GetMapping("/{missionId}/qna/{qnaId}")
    public ApiResponse<MissionQnAResponse> getQnADetail(
            @PathVariable Long missionId,
            @PathVariable Long qnaId) {
        MissionQnAResponse qna = missionService.getQnADetail(missionId, qnaId);
        return ApiResponse.success(qna);
    }

    @Operation(summary = "미션 QnA 질문 작성")
    @PostMapping("/{missionId}/qna")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionQnAResponse> createQuestion(
            @PathVariable Long missionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid MissionQnARequest request) {
        MissionQnAResponse qna = missionService.createQuestion(missionId, userId, request);
        return ApiResponse.success(qna);
    }

    @Operation(summary = "미션 QnA 답변 작성")
    @PostMapping("/{missionId}/qna/{qnaId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MissionQnAResponse.AnswerInfo> createAnswer(
            @PathVariable Long missionId,
            @PathVariable Long qnaId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid MissionQnAAnswerRequest request) {
        MissionQnAResponse.AnswerInfo answer = missionService.createAnswer(missionId, qnaId, userId, request);
        return ApiResponse.success(answer);
    }

    @Operation(summary = "미션 QnA 답변 채택")
    @PutMapping("/{missionId}/qna/{qnaId}/answers/{answerId}/accept")
    public ApiResponse<Map<String, Object>> acceptAnswer(
            @PathVariable Long missionId,
            @PathVariable Long qnaId,
            @PathVariable Long answerId,
            @AuthenticationPrincipal Long userId) {
        missionService.acceptAnswer(missionId, qnaId, answerId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", answerId);
        result.put("qnaId", qnaId);
        result.put("isAccepted", true);
        result.put("message", "답변이 채택되었습니다.");

        return ApiResponse.success(result);
    }
}
