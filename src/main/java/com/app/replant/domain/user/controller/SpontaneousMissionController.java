package com.app.replant.domain.user.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.user.dto.SpontaneousMissionRequest;
import com.app.replant.domain.user.dto.SpontaneousMissionResponse;
import com.app.replant.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Spontaneous Mission", description = "돌발 미션 설정 API")
@RestController
@RequestMapping("/api/spontaneous-missions")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT Token")
public class SpontaneousMissionController {

    private final UserService userService;

    @Operation(summary = "돌발 미션 설정 조회", description = "현재 사용자의 기상/취침/식사 시간 설정을 조회합니다. 설정이 없으면 404.")
    @GetMapping("/setup")
    public ApiResponse<SpontaneousMissionResponse> getSetup(@AuthenticationPrincipal Long userId) {
        SpontaneousMissionResponse response = userService.getSpontaneousMissionSetup(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "돌발 미션 설정 등록", description = "기상/취침/식사 시간을 최초 등록합니다.")
    @PostMapping("/setup")
    public ApiResponse<SpontaneousMissionResponse> setup(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SpontaneousMissionRequest request) {
        SpontaneousMissionResponse response = userService.setupSpontaneousMission(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "돌발 미션 설정 수정", description = "기상/취침/식사 시간을 수정합니다.")
    @PutMapping("/setup")
    public ApiResponse<SpontaneousMissionResponse> updateSetup(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SpontaneousMissionRequest request) {
        SpontaneousMissionResponse response = userService.updateSpontaneousMissionSetup(userId, request);
        return ApiResponse.success(response);
    }
}
