package com.app.replant.domain.post.service;

import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.custommission.repository.CustomMissionRepository;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.domain.post.dto.CommentRequest;
import com.app.replant.domain.post.dto.CommentResponse;
import com.app.replant.domain.post.dto.PostRequest;
import com.app.replant.domain.post.dto.PostResponse;
import com.app.replant.domain.post.entity.Comment;
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.repository.CommentRepository;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
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

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final CustomMissionRepository customMissionRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public Page<PostResponse> getPosts(Long missionId, Long customMissionId, Boolean badgeOnly, Pageable pageable) {
        boolean badgeFilter = badgeOnly != null && badgeOnly;
        return postRepository.findWithFilters(missionId, customMissionId, badgeFilter, pageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    return PostResponse.from(post, commentCount);
                });
    }

    public PostResponse getPost(Long postId) {
        Post post = findPostById(postId);
        long commentCount = commentRepository.countByPostId(postId);
        return PostResponse.from(post, commentCount);
    }

    @Transactional
    public PostResponse createPost(Long userId, PostRequest request) {
        User user = findUserById(userId);
        Mission mission = null;
        CustomMission customMission = null;

        if (request.getMissionId() != null) {
            mission = missionRepository.findById(request.getMissionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));
        } else if (request.getCustomMissionId() != null) {
            customMission = customMissionRepository.findById(request.getCustomMissionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_MISSION_NOT_FOUND));
        }

        // Check if user has valid badge for the mission
        boolean hasValidBadge = false;
        if (mission != null) {
            hasValidBadge = userBadgeRepository.hasValidBadgeForMission(userId, mission.getId(), LocalDateTime.now());
        }

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_DATA);
            }
        }

        Post post = Post.builder()
                .user(user)
                .mission(mission)
                .customMission(customMission)
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrls(imageUrlsJson)
                .hasValidBadge(hasValidBadge)
                .build();

        Post saved = postRepository.save(post);
        return PostResponse.from(saved, 0L);
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
        return PostResponse.from(post, commentCount);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = findPostById(postId);

        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // Soft delete (delFlag를 true로 설정)
        post.softDelete();
    }

    // Comment methods
    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        // Verify post exists
        findPostById(postId);
        // 최상위 댓글만 조회하고, 대댓글은 replies로 포함
        return commentRepository.findParentCommentsByPostId(postId, pageable)
                .map(CommentResponse::fromWithReplies);
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentRequest request) {
        Post post = findPostById(postId);
        User user = findUserById(userId);

        // 부모 댓글 처리
        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = findCommentById(request.getParentId());
            // 부모 댓글이 같은 게시글에 속하는지 확인
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

        // 댓글 알림 발송 (본인 글에 댓글 달면 알림 안 함)
        if (!post.getUser().getId().equals(userId)) {
            sendCommentNotification(post.getUser(), user, post);
        }

        // 대댓글인 경우, 부모 댓글 작성자에게도 알림 (본인이 아닌 경우)
        if (parentComment != null && !parentComment.getUser().getId().equals(userId)) {
            sendReplyNotification(parentComment.getUser(), user, post);
        }

        return CommentResponse.from(saved);
    }

    /**
     * 대댓글 알림 발송
     */
    private void sendReplyNotification(User parentCommentAuthor, User replier, Post post) {
        String title = "댓글에 답글이 달렸습니다";
        String content = String.format("%s님이 회원님의 댓글에 답글을 달았습니다.",
                replier.getNickname());

        notificationService.createAndPushNotification(
                parentCommentAuthor,
                "REPLY",
                title,
                content,
                "POST",
                post.getId()
        );

        log.info("답글 알림 발송 - postId={}, replierId={}, parentCommentAuthorId={}",
                post.getId(), replier.getId(), parentCommentAuthor.getId());
    }

    /**
     * 댓글 알림 발송
     */
    private void sendCommentNotification(User postAuthor, User commenter, Post post) {
        String title = "새로운 댓글이 달렸습니다";
        String content = String.format("%s님이 '%s' 게시글에 댓글을 달았습니다.",
                commenter.getNickname(), truncateTitle(post.getTitle(), 20));

        notificationService.createAndPushNotification(
                postAuthor,
                "COMMENT",
                title,
                content,
                "POST",
                post.getId()
        );

        log.info("댓글 알림 발송 - postId={}, commenterId={}, postAuthorId={}",
                post.getId(), commenter.getId(), postAuthor.getId());
    }

    /**
     * 제목 자르기 (길면 ... 추가)
     */
    private String truncateTitle(String title, int maxLength) {
        if (title == null) return "게시글";
        if (title.length() <= maxLength) return title;
        return title.substring(0, maxLength) + "...";
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
