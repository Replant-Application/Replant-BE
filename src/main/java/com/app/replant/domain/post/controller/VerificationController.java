package com.app.replant.domain.post.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.post.dto.PostResponse;
import com.app.replant.domain.post.dto.VerificationPostRequest;
import com.app.replant.domain.post.dto.VerificationPostUpdateRequest;
import com.app.replant.domain.post.service.PostService;
import com.app.replant.domain.usermission.dto.TimeVerifyRequest;
import com.app.replant.domain.usermission.dto.VerifyMissionResponse;
import com.app.replant.domain.usermission.service.UserMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Verification", description = "인증 게시판 API")
@Slf4j
@RestController
@RequestMapping("/api/verifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT Token")
public class VerificationController {

    private final PostService postService;
    private final UserMissionService userMissionService;

    @Operation(summary = "인증글 목록 조회",
            description = "인증 게시글 목록을 조회합니다. status로 필터링 가능 (PENDING, APPROVED)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "content": [
                                  {
                                    "id": 1,
                                    "postType": "VERIFICATION",
                                    "userId": 10,
                                    "userNickname": "사용자닉네임",
                                    "missionTag": {
                                      "id": 5,
                                      "title": "아침 러닝 30분",
                                      "type": "OFFICIAL"
                                    },
                                    "content": "오늘 아침 30분 러닝 완료!",
                                    "imageUrls": ["https://..."],
                                    "likeCount": 3,
                                    "commentCount": 2,
                                    "status": "APPROVED",
                                    "createdAt": "2024-01-15T07:30:00"
                                  }
                                ],
                                "totalElements": 1,
                                "totalPages": 1,
                                "number": 0
                              }
                            }
                            """)))
    })
    @GetMapping
    public ApiResponse<Page<PostResponse>> getVerificationPosts(
            @Parameter(description = "인증 상태 필터 (PENDING, APPROVED)")
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostResponse> posts = postService.getVerificationPosts(status, pageable, userId);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "시간 인증",
            description = "시간 인증 미션을 완료합니다. 돌발 미션(기상/식사)과 일반 투두리스트 TIME 인증 모두 지원.")
    @PostMapping("/time")
    public ApiResponse<VerifyMissionResponse> verifyByTime(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid TimeVerifyRequest request) {
        log.info("[인증] 시간 인증 요청 - userId: {}, userMissionId: {} (기상/식사/투두 TIME 인증)", userId, request.getUserMissionId());
        VerifyMissionResponse response = userMissionService.verifyByTime(
                request.getUserMissionId(), userId);
        log.info("[인증] 시간 인증 완료 - userId: {}, userMissionId: {}", userId, request.getUserMissionId());
        return ApiResponse.success(response);
    }

    @Operation(summary = "인증글 상세 조회")
    @GetMapping("/{verificationId}")
    public ApiResponse<PostResponse> getVerificationPost(
            @Parameter(description = "인증글 ID", example = "1")
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId) {
        PostResponse post = postService.getVerificationPost(verificationId, userId);
        return ApiResponse.success(post);
    }

    @Operation(summary = "인증글 작성",
            description = "새 인증글을 작성합니다. userMissionId 필수. 로그인 필요.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "userMissionId": 15,
                      "content": "오늘 아침 30분 러닝 완료했습니다! 기분이 상쾌해요~",
                      "imageUrls": ["https://example.com/image1.jpg"]
                    }
                    """))
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createVerificationPost(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VerificationPostRequest request) {
        PostResponse post = postService.createVerificationPost(userId, request);
        return ApiResponse.success(post);
    }

    @Operation(summary = "인증글 수정",
            description = "인증글을 수정합니다. PENDING 상태만 수정 가능합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "content": "수정된 인증 내용",
                      "imageUrls": ["https://example.com/image1.jpg"],
                      "completionRate": 75
                    }
                    """))
    )
    @PutMapping("/{verificationId}")
    public ApiResponse<PostResponse> updateVerificationPost(
            @Parameter(description = "인증글 ID", example = "1")
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VerificationPostUpdateRequest request) {
        PostResponse post = postService.updateVerificationPost(verificationId, userId, request);
        return ApiResponse.success(post);
    }

    @Operation(summary = "인증글에 좋아요 (= 인증 투표)",
            description = "인증글에 좋아요를 누릅니다. 좋아요 수가 기준치 이상이면 자동 인증 완료됩니다.")
    @PostMapping("/{verificationId}/votes")
    public ApiResponse<Map<String, Object>> voteVerification(
            @Parameter(description = "인증글 ID", example = "1")
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId) {
        Map<String, Object> result = postService.toggleLike(verificationId, userId);
        return ApiResponse.success(result);
    }
}
