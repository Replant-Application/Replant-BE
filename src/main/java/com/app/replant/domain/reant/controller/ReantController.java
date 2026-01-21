package com.app.replant.domain.reant.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.reant.dto.ReantResponse;
import com.app.replant.domain.reant.dto.ReantStatusResponse;
import com.app.replant.domain.reant.dto.ReantUpdateRequest;
import com.app.replant.domain.reant.service.ReantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reant", description = "펫 관련 API")
@RestController
@RequestMapping("/api/reant")
@RequiredArgsConstructor
public class ReantController {

    private final ReantService reantService;

    @Operation(summary = "내 펫 조회")
    @GetMapping
    public ApiResponse<ReantResponse> getMyReant(@AuthenticationPrincipal Long userId) {
        ReantResponse response = reantService.getMyReant(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "펫 정보 수정")
    @PutMapping
    public ApiResponse<ReantResponse> updateReant(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ReantUpdateRequest request) {
        ReantResponse response = reantService.updateReant(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "펫 상태 조회", description = "기분, 건강, 배고픔 등 상세 상태 정보 조회")
    @GetMapping("/status")
    public ApiResponse<ReantStatusResponse> getReantStatus(@AuthenticationPrincipal Long userId) {
        ReantStatusResponse response = reantService.getReantStatus(userId);
        return ApiResponse.success(response);
    }
}
