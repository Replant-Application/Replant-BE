package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.post.dto.CommentRequest;
import com.app.replant.domain.post.dto.CommentResponse;
import com.app.replant.domain.post.entity.Comment;
import com.app.replant.domain.post.repository.CommentRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.verification.dto.*;
import com.app.replant.domain.verification.entity.VerificationPost;
import com.app.replant.domain.verification.enums.VerificationStatus;
import com.app.replant.domain.verification.repository.VerificationPostRepository;
import com.app.replant.domain.verification.service.VerificationService;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
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
    private final VerificationPostRepository verificationPostRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

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

        // 시작/종료 시간 파싱 (옵션)
        java.time.LocalDateTime startedAt = null;
        java.time.LocalDateTime endedAt = null;

        if (request.containsKey("startedAt") && request.get("startedAt") != null) {
            startedAt = java.time.LocalDateTime.parse(request.get("startedAt").toString());
        }
        if (request.containsKey("endedAt") && request.get("endedAt") != null) {
            endedAt = java.time.LocalDateTime.parse(request.get("endedAt").toString());
        }

        Map<String, Object> result = verificationService.verifyByTime(userId, userMissionId, startedAt, endedAt);
        return ApiResponse.success(result);
    }

    // ============================================
    // 인증글 댓글 API
    // ============================================

    @Operation(summary = "인증글 댓글 목록 조회")
    @GetMapping("/{verificationId}/comments")
    public ApiResponse<Page<CommentResponse>> getVerificationComments(
            @PathVariable Long verificationId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        // 인증글 존재 확인
        verificationPostRepository.findById(verificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));

        Page<CommentResponse> comments = commentRepository.findParentCommentsByVerificationPostId(verificationId, pageable)
                .map(CommentResponse::fromWithReplies);
        return ApiResponse.success(comments);
    }

    @Operation(summary = "인증글 댓글 작성")
    @PostMapping("/{verificationId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createVerificationComment(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CommentRequest request) {
        VerificationPost verificationPost = verificationPostRepository.findById(verificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 부모 댓글 처리
        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
            // 부모 댓글이 같은 인증글에 속하는지 확인
            if (parentComment.getVerificationPost() == null ||
                !parentComment.getVerificationPost().getId().equals(verificationId)) {
                throw new CustomException(ErrorCode.INVALID_PARENT_COMMENT);
            }
        }

        Comment comment = Comment.builder()
                .verificationPost(verificationPost)
                .user(user)
                .content(request.getContent())
                .parent(parentComment)
                .build();

        Comment saved = commentRepository.save(comment);
        return ApiResponse.success(CommentResponse.from(saved));
    }

    @Operation(summary = "인증글 댓글 수정")
    @PutMapping("/{verificationId}/comments/{commentId}")
    public ApiResponse<CommentResponse> updateVerificationComment(
            @PathVariable Long verificationId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        comment.updateContent(request.getContent());
        return ApiResponse.success(CommentResponse.from(comment));
    }

    @Operation(summary = "인증글 댓글 삭제")
    @DeleteMapping("/{verificationId}/comments/{commentId}")
    public ApiResponse<Map<String, String>> deleteVerificationComment(
            @PathVariable Long verificationId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        commentRepository.delete(comment);

        Map<String, String> result = new HashMap<>();
        result.put("message", "댓글이 삭제되었습니다.");
        return ApiResponse.success(result);
    }

    @Operation(summary = "인증글 댓글 수 조회")
    @GetMapping("/{verificationId}/comments/count")
    public ApiResponse<Map<String, Long>> getVerificationCommentCount(@PathVariable Long verificationId) {
        long count = commentRepository.countByVerificationPostId(verificationId);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ApiResponse.success(result);
    }
}
