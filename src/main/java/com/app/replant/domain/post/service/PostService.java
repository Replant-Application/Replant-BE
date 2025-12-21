package com.app.replant.domain.post.service;

import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.custommission.repository.CustomMissionRepository;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.repository.MissionRepository;
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
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final CustomMissionRepository customMissionRepository;
    private final UserBadgeRepository userBadgeRepository;
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

        postRepository.delete(post);
    }

    // Comment methods
    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        // Verify post exists
        findPostById(postId);
        return commentRepository.findByPostId(postId, pageable)
                .map(CommentResponse::from);
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentRequest request) {
        Post post = findPostById(postId);
        User user = findUserById(userId);

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(comment);
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

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
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
