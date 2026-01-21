package com.app.replant.domain.user.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.user.dto.SpontaneousMissionRequest;
import com.app.replant.domain.user.dto.SpontaneousMissionResponse;
import com.app.replant.domain.user.service.SpontaneousMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "SpontaneousMission", description = "돌발 미션 설정 API")
@RestController
@RequestMapping("/api/spontaneous-missions")
@RequiredArgsConstructor
public class SpontaneousMissionController {

    private final SpontaneousMissionService spontaneousMissionService;

    @Operation(summary = "돌발 미션 설정 조회", description = "사용자의 돌발 미션 설정 완료 여부와 설정된 시간을 조회합니다.")
    @GetMapping("/setup")
    public ApiResponse<SpontaneousMissionResponse> getSpontaneousMissionSetup(
            @AuthenticationPrincipal Long userId) {
        SpontaneousMissionResponse response = spontaneousMissionService.getSpontaneousMissionSetup(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "돌발 미션 설정 제출", description = "돌발 미션 설정을 제출합니다. 취침 시간, 기상 시간, 식사 시간을 설정합니다.")
    @PostMapping("/setup")
    public ApiResponse<SpontaneousMissionResponse> setupSpontaneousMission(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SpontaneousMissionRequest request) {
        SpontaneousMissionResponse response = spontaneousMissionService.setupSpontaneousMission(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "돌발 미션 설정 수정", description = "설정된 취침 시간, 기상 시간, 식사 시간을 수정합니다.")
    @PutMapping("/setup")
    public ApiResponse<SpontaneousMissionResponse> updateSpontaneousMissionSetup(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SpontaneousMissionRequest request) {
        SpontaneousMissionResponse response = spontaneousMissionService.updateSpontaneousMissionSetup(userId, request);
        return ApiResponse.success(response);
    }
}
