package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.post.dto.CommentRequest;
import com.app.replant.domain.post.dto.CommentResponse;
import com.app.replant.domain.post.entity.Comment;
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.repository.CommentRepository;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.verification.dto.*;
import com.app.replant.domain.verification.enums.VerificationStatus;
import com.app.replant.domain.verification.service.VerificationService;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Verification", description = "인증 API")
@RestController
@RequestMapping("/api/verifications")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final VerificationService verificationService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

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
        // NPE 방어: 필수 파라미터 null 체크
        if (request.get("userMissionId") == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "userMissionId는 필수입니다.");
        }
        if (request.get("latitude") == null || request.get("longitude") == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "GPS 좌표(latitude, longitude)는 필수입니다.");
        }

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
        // NPE 방어: 필수 파라미터 null 체크
        if (request.get("userMissionId") == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "userMissionId는 필수입니다.");
        }
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
    public ApiResponse<Map<String, Object>> getVerificationComments(
            @PathVariable Long verificationId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        // 인증글 존재 확인
        postRepository.findVerificationPostById(verificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));

        // User까지 함께 로딩하는 쿼리 사용 (LAZY 로딩 문제 해결)
        List<Comment> allComments = commentRepository.findParentCommentsByPostIdWithUser(verificationId);
        List<CommentResponse> commentResponses = allComments.stream()
                .map(CommentResponse::fromWithReplies)
                .collect(java.util.stream.Collectors.toList());

        // 페이징 정보를 수동으로 구성
        Map<String, Object> result = new HashMap<>();
        result.put("content", commentResponses);
        result.put("totalElements", commentResponses.size());
        result.put("totalPages", 1);
        result.put("number", 0);

        return ApiResponse.success(result);
    }

    @Operation(summary = "인증글 댓글 작성")
    @PostMapping("/{verificationId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createVerificationComment(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CommentRequest request) {
        Post post = postRepository.findVerificationPostById(verificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 부모 댓글 처리
        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
            // 부모 댓글이 같은 게시글에 속하는지 확인
            if (parentComment.getPost() == null ||
                !parentComment.getPost().getId().equals(verificationId)) {
                throw new CustomException(ErrorCode.INVALID_PARENT_COMMENT);
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .parent(parentComment)
                .build();

        Comment saved = commentRepository.save(comment);

        // 댓글 알림 발송 (본인 글에 댓글 달면 알림 안 함)
        if (post.getUser() != null && !post.getUser().getId().equals(userId)) {
            sendVerificationCommentNotification(post.getUser(), user, post);
        }

        // 대댓글인 경우, 부모 댓글 작성자에게도 알림 (본인이 아닌 경우)
        if (parentComment != null && parentComment.getUser() != null && !parentComment.getUser().getId().equals(userId)) {
            sendVerificationReplyNotification(parentComment.getUser(), user, post);
        }

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
        long count = commentRepository.countByPostId(verificationId);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ApiResponse.success(result);
    }

    /**
     * 인증글 댓글 알림 발송
     */
    private void sendVerificationCommentNotification(User postAuthor, User commenter, Post post) {
        String missionTitle = getMissionTitle(post);
        String title = "인증글에 댓글이 달렸습니다";
        String content = String.format("%s님이 '%s' 인증글에 댓글을 달았습니다.",
                commenter.getNickname(), truncateTitle(missionTitle, 15));

        notificationService.createAndPushNotification(
                postAuthor,
                NotificationType.VERIFICATION_COMMENT,
                title,
                content,
                "VERIFICATION",
                post.getId()
        );

        log.info("인증글 댓글 알림 발송 - verificationId={}, commenterId={}, postAuthorId={}",
                post.getId(), commenter.getId(), postAuthor.getId());
    }

    /**
     * 인증글 대댓글 알림 발송
     */
    private void sendVerificationReplyNotification(User parentCommentAuthor, User replier, Post post) {
        String title = "인증글 댓글에 답글이 달렸습니다";
        String content = String.format("%s님이 회원님의 댓글에 답글을 달았습니다.", replier.getNickname());

        notificationService.createAndPushNotification(
                parentCommentAuthor,
                NotificationType.VERIFICATION_REPLY,
                title,
                content,
                "VERIFICATION",
                post.getId()
        );

        log.info("인증글 답글 알림 발송 - verificationId={}, replierId={}, parentCommentAuthorId={}",
                post.getId(), replier.getId(), parentCommentAuthor.getId());
    }

    /**
     * 미션 제목 가져오기
     */
    private String getMissionTitle(Post post) {
        if (post.getUserMission() != null) {
            if (post.getUserMission().getMission() != null) {
                return post.getUserMission().getMission().getTitle();
            } else if (post.getUserMission().getCustomMission() != null) {
                return post.getUserMission().getCustomMission().getTitle();
            }
        }
        return "미션";
    }

    /**
     * 제목 자르기
     */
    private String truncateTitle(String title, int maxLength) {
        if (title == null) return "인증글";
        if (title.length() <= maxLength) return title;
        return title.substring(0, maxLength) + "...";
    }
}
