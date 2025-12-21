package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.recommendation.dto.AcceptRecommendationResponse;
import com.app.replant.domain.recommendation.dto.RecommendationResponse;
import com.app.replant.domain.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Recommendation", description = "유저 추천 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "추천 목록 조회")
    @GetMapping
    public ApiResponse<List<RecommendationResponse>> getRecommendations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String status) {
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, status);
        return ApiResponse.success(recommendations);
    }

    @Operation(summary = "추천 수락 (채팅방 자동 생성)")
    @PostMapping("/{recommendationId}/accept")
    public ApiResponse<AcceptRecommendationResponse> acceptRecommendation(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal Long userId) {
        AcceptRecommendationResponse response = recommendationService.acceptRecommendation(recommendationId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "추천 거절")
    @PostMapping("/{recommendationId}/reject")
    public ApiResponse<Map<String, String>> rejectRecommendation(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal Long userId) {
        recommendationService.rejectRecommendation(recommendationId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("recommendationId", String.valueOf(recommendationId));
        result.put("status", "REJECTED");
        result.put("message", "추천을 거절했습니다.");

        return ApiResponse.success(result);
    }
}
