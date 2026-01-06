package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.verification.dto.*;
import com.app.replant.domain.verification.enums.VerificationStatus;
import com.app.replant.domain.verification.service.VerificationService;
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

@Tag(name = "Verification", description = "인증 게시판 API")
@RestController
@RequestMapping("/api/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @Operation(summary = "인증글 목록 조회")
    @GetMapping
    public ApiResponse<Page<VerificationPostResponse>> getVerifications(
            @RequestParam(required = false) VerificationStatus status,
            @RequestParam(required = false) Long missionId,
            @RequestParam(required = false) Long customMissionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<VerificationPostResponse> verifications = verificationService.getVerifications(status, missionId, customMissionId, pageable);
        return ApiResponse.success(verifications);
    }

    @Operation(summary = "인증글 상세 조회")
    @GetMapping("/{verificationId}")
    public ApiResponse<VerificationPostResponse> getVerification(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId) {
        VerificationPostResponse verification = verificationService.getVerification(verificationId, userId);
        return ApiResponse.success(verification);
    }

    @Operation(summary = "인증글 작성 (COMMUNITY 타입)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VerificationPostResponse> createVerification(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VerificationPostRequest request) {
        VerificationPostResponse verification = verificationService.createVerification(userId, request);
        return ApiResponse.success(verification);
    }

    @Operation(summary = "인증글 수정")
    @PutMapping("/{verificationId}")
    public ApiResponse<VerificationPostResponse> updateVerification(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VerificationPostRequest request) {
        VerificationPostResponse verification = verificationService.updateVerification(verificationId, userId, request);
        return ApiResponse.success(verification);
    }

    @Operation(summary = "인증글 삭제")
    @DeleteMapping("/{verificationId}")
    public ApiResponse<Map<String, String>> deleteVerification(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId) {
        verificationService.deleteVerification(verificationId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "인증글이 삭제되었습니다.");

        return ApiResponse.success(result);
    }

    @Operation(summary = "인증 투표 (좋아요/싫어요)")
    @PostMapping("/{verificationId}/votes")
    public ApiResponse<VoteResponse> vote(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VoteRequest request) {
        VoteResponse response = verificationService.vote(verificationId, userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "GPS 인증 (GPS 타입 미션)")
    @PostMapping("/gps")
    public ApiResponse<Map<String, Object>> verifyByGps(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> request) {
        Long userMissionId = Long.valueOf(request.get("userMissionId").toString());
        Double latitude = Double.valueOf(request.get("latitude").toString());
        Double longitude = Double.valueOf(request.get("longitude").toString());

        Map<String, Object> result = verificationService.verifyByGps(userId, userMissionId, latitude, longitude);
        return ApiResponse.success(result);
    }

    @Operation(summary = "시간 인증 (TIME 타입 미션)")
    @PostMapping("/time")
    public ApiResponse<Map<String, Object>> verifyByTime(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> request) {
        Long userMissionId = Long.valueOf(request.get("userMissionId").toString());

        Map<String, Object> result = verificationService.verifyByTime(userId, userMissionId);
        return ApiResponse.success(result);
    }
}
