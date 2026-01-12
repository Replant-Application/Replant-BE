package com.app.replant.domain.post.service;

import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.domain.post.dto.CommentRequest;
import com.app.replant.domain.post.dto.CommentResponse;
import com.app.replant.domain.post.dto.PostRequest;
import com.app.replant.domain.post.dto.PostResponse;
import com.app.replant.domain.post.entity.Comment;
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.entity.PostLike;
import com.app.replant.domain.post.repository.CommentRepository;
import com.app.replant.domain.post.repository.PostLikeRepository;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.app.replant.domain.usermission.service.UserMissionService userMissionService;
    private final ObjectMapper objectMapper;

    // ========================================
    // 게시글 CRUD
    // ========================================

    public Page<PostResponse> getPosts(Long missionId, Long customMissionId, Boolean badgeOnly, Pageable pageable) {
        return getPosts(missionId, customMissionId, badgeOnly, pageable, null);
    }

    public Page<PostResponse> getPosts(Long missionId, Long customMissionId, Boolean badgeOnly, Pageable pageable,
            Long currentUserId) {
        boolean badgeFilter = badgeOnly != null && badgeOnly;
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        return postRepository.findWithFilters(missionId, customMissionId, badgeFilter, pageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    long likeCount = postLikeRepository.countByPostId(post.getId());
                    boolean isLiked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);
                    return PostResponse.from(post, commentCount, likeCount, isLiked);
                });
    }

    public PostResponse getPost(Long postId) {
        return getPost(postId, null);
    }

    public PostResponse getPost(Long postId, Long currentUserId) {
        Post post = findPostById(postId);
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        long commentCount = commentRepository.countByPostId(postId);
        long likeCount = postLikeRepository.countByPostId(postId);
        boolean isLiked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);

        return PostResponse.from(post, commentCount, likeCount, isLiked);
    }

    /**
     * 일반 게시글 생성
     */
    @Transactional
    public PostResponse createPost(Long userId, PostRequest request) {
        User user = findUserById(userId);

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_DATA);
            }
        }

        Post post = Post.generalBuilder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrls(imageUrlsJson)
                .build();

        Post saved = postRepository.save(post);
        return PostResponse.from(saved, 0L, 0L, false);
    }

    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostRequest request) {
        Post post = findPostById(postId);

        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        String imageUrlsJson = null;
        if (request.getImageUrls() != null) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_DATA);
            }
        }

        post.update(request.getTitle(), request.getContent(), imageUrlsJson);
        long commentCount = commentRepository.countByPostId(postId);
        long likeCount = postLikeRepository.countByPostId(postId);
        return PostResponse.from(post, commentCount, likeCount, false);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = findPostById(postId);

        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        post.softDelete();
    }

    // ========================================
    // 댓글 CRUD
    // ========================================

    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        findPostById(postId);
        List<Comment> comments = commentRepository.findParentCommentsByPostIdWithUser(postId);
        List<CommentResponse> responseList = comments.stream()
                .map(CommentResponse::fromWithReplies)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responseList.size());
        List<CommentResponse> pagedList = start < responseList.size() ? responseList.subList(start, end)
                : Collections.emptyList();

        return new org.springframework.data.domain.PageImpl<>(pagedList, pageable, responseList.size());
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentRequest request) {
        Post post = findPostById(postId);
        User user = findUserById(userId);

        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = findCommentById(request.getParentId());
            if (!parentComment.getPost().getId().equals(postId)) {
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

        // 댓글 알림
        if (!post.getUser().getId().equals(userId)) {
            sendCommentNotification(post.getUser(), user, post);
        }

        // 대댓글 알림
        if (parentComment != null && !parentComment.getUser().getId().equals(userId)) {
            sendReplyNotification(parentComment.getUser(), user, post);
        }

        return CommentResponse.from(saved);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest request) {
        Comment comment = findCommentById(commentId);

        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        comment.updateContent(request.getContent());
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = findCommentById(commentId);

        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        commentRepository.delete(comment);
    }

    // ========================================
    // 좋아요 (= 인증)
    // ========================================

    /**
     * 좋아요 토글
     * - VERIFICATION 타입: 좋아요 수가 임계값 이상이면 자동 인증 + 알림
     * - GENERAL 타입: 단순 좋아요
     */
    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        Post post = findPostById(postId);
        User user = findUserById(userId);

        // 자기 글에는 좋아요 불가
        if (post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.CANNOT_LIKE_OWN_POST);
        }

        Map<String, Object> result = new HashMap<>();
        boolean isLiked;
        boolean newlyVerified = false;

        Optional<PostLike> existingLike = postLikeRepository.findByPostAndUser(post, user);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            postLikeRepository.delete(existingLike.get());
            isLiked = false;
            log.info("좋아요 취소 - postId={}, userId={}", postId, userId);
        } else {
            // 좋아요 추가
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(newLike);
            isLiked = true;
            log.info("좋아요 추가 - postId={}, userId={}", postId, userId);

            // 좋아요 알림 (임시 비활성화 - notification 테이블 마이그레이션 필요)
            // sendLikeNotification(post.getUser(), user, post);

            // VERIFICATION 타입: 좋아요 = 인증 체크
            if (post.isVerificationPost()) {
                long likeCount = postLikeRepository.countByPostId(postId);
                newlyVerified = post.checkAndApproveByLikes(likeCount);

                if (newlyVerified) {
                    log.info("인증 완료! postId={}, likeCount={}", postId, likeCount);
                    // 인증 완료 처리 (뱃지, 경험치 등)
                    userMissionService.completeMissionVerification(post.getUserMission());

                    // 알림 전송
                    sendVerificationSuccessNotification(post.getUser(), post);
                }
            }
        }

        long likeCount = postLikeRepository.countByPostId(postId);

        result.put("isLiked", isLiked);
        result.put("likeCount", likeCount);
        result.put("verified", newlyVerified);

        return result;
    }

    // ========================================
    // 알림 메서드
    // ========================================

    private void sendCommentNotification(User postAuthor, User commenter, Post post) {
        String title = "새로운 댓글이 달렸습니다";
        String content = String.format("%s님이 '%s' 게시글에 댓글을 달았습니다.",
                commenter.getNickname(), truncateTitle(post.getTitle(), 20));

        notificationService.createAndPushNotification(
                postAuthor,
                NotificationType.COMMENT,
                title,
                content,
                "POST",
                post.getId());
    }

    private void sendReplyNotification(User parentCommentAuthor, User replier, Post post) {
        String title = "댓글에 답글이 달렸습니다";
        String content = String.format("%s님이 회원님의 댓글에 답글을 달았습니다.",
                replier.getNickname());

        notificationService.createAndPushNotification(
                parentCommentAuthor,
                NotificationType.REPLY,
                title,
                content,
                "POST",
                post.getId());
    }

    private void sendLikeNotification(User postAuthor, User liker, Post post) {
        String title = "게시글에 좋아요가 달렸습니다";
        String postTitle = post.isVerificationPost()
                ? (post.getMissionTitle() != null ? post.getMissionTitle() + " 인증" : "미션 인증")
                : truncateTitle(post.getTitle(), 20);
        String content = String.format("%s님이 '%s' 게시글에 좋아요를 눌렀습니다.",
                liker.getNickname(), postTitle);

        notificationService.createAndPushNotification(
                postAuthor,
                NotificationType.LIKE,
                title,
                content,
                "POST",
                post.getId());
    }

    private void sendVerificationSuccessNotification(User postAuthor, Post post) {
        String title = "미션 인증이 완료되었습니다!";
        String content = String.format("'%s' 미션 인증이 완료되어 뱃지를 획득했습니다.",
                post.getMissionTitle() != null ? post.getMissionTitle() : "미션");

        notificationService.createAndPushNotification(
                postAuthor,
                NotificationType.VERIFICATION_APPROVED,
                title,
                content,
                "POST",
                post.getId());
    }

    private String truncateTitle(String title, int maxLength) {
        if (title == null)
            return "게시글";
        if (title.length() <= maxLength)
            return title;
        return title.substring(0, maxLength) + "...";
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    private Post findPostById(Long postId) {
        return postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
