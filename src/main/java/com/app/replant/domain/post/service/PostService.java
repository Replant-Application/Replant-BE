package com.app.replant.domain.post.service;

import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.domain.post.dto.CommentRequest;
import com.app.replant.domain.post.dto.CommentResponse;
import com.app.replant.domain.post.dto.PostRequest;
import com.app.replant.domain.post.dto.PostResponse;
import com.app.replant.domain.post.dto.VerificationPostRequest;
import com.app.replant.domain.post.enums.PostType;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.domain.post.entity.Comment;
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.entity.PostLike;
import com.app.replant.domain.post.repository.CommentRepository;
import com.app.replant.domain.post.repository.PostLikeRepository;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import com.app.replant.global.filter.BadWordFilterService;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostService {

    // 정렬 가능한 필드명 목록
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "createdAt", "updatedAt", "status", "verifiedAt"
    );

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final UserMissionRepository userMissionRepository;
    private final NotificationService notificationService;
    private final com.app.replant.domain.usermission.service.UserMissionService userMissionService;
    private final ObjectMapper objectMapper;
    private final BadWordFilterService badWordFilterService;

    // ========================================
    // 게시글 CRUD
    // ========================================

    public Page<PostResponse> getPosts(Long missionId, Boolean badgeOnly, Pageable pageable) {
        return getPosts(missionId, badgeOnly, pageable, null);
    }

    public Page<PostResponse> getPosts(Long missionId, Boolean badgeOnly, Pageable pageable,
            Long currentUserId) {
        boolean badgeFilter = badgeOnly != null && badgeOnly;
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        return postRepository.findWithFilters(missionId, badgeFilter, pageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    long likeCount = postLikeRepository.countByPostId(post.getId());
                    boolean isLiked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);
                    return PostResponse.from(post, commentCount, likeCount, isLiked, currentUserId);
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

        return PostResponse.from(post, commentCount, likeCount, isLiked, currentUserId);
    }

    /**
     * 일반 게시글 생성
     */
    @Transactional
    public PostResponse createPost(Long userId, PostRequest request) {
        log.info("일반 게시글 생성 호출 - userId={}, title={}", userId, request.getTitle());
        User user = findUserById(userId);

        // 비속어 필터링
        validateBadWords(request.getTitle(), request.getContent());

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
        
        log.debug("일반 게시글 생성 전 - postType={}, userId={}, title={}", post.getPostType(), userId, request.getTitle());

        Post saved = postRepository.save(post);
        log.info("일반 게시글 생성 완료 - postId={}, postType={}, userId={}, title={}", 
                saved.getId(), saved.getPostType(), userId, saved.getTitle());
        return PostResponse.from(saved, 0L, 0L, false, userId);
    }

    /**
     * 인증 게시글 생성 (VERIFICATION 타입)
     */
    @Transactional
    public PostResponse createVerificationPost(Long userId, VerificationPostRequest request) {
        User user = findUserById(userId);

        // 비속어 필터링
        validateBadWords(null, request.getContent());

        // UserMission 조회
        UserMission userMission = userMissionRepository.findByIdAndUserId(request.getUserMissionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        // 이미 해당 미션에 대한 인증글이 있는지 확인 (동시 요청 방지를 위해 flush 후 재확인)
        postRepository.flush(); // 현재 트랜잭션의 변경사항을 DB에 반영
        if (postRepository.findByUserMissionId(userMission.getId()).isPresent()) {
            throw new CustomException(ErrorCode.VERIFICATION_ALREADY_EXISTS);
        }

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_DATA);
            }
        }

        // VERIFICATION 타입 게시글 생성
        Post post = Post.createVerificationPost(user, userMission, request.getContent(), imageUrlsJson, request.getCompletionRate());
        log.debug("인증 게시글 생성 전 - postType={}, userId={}, userMissionId={}", post.getPostType(), userId, userMission.getId());

        // UserMission 상태를 PENDING(인증대기)으로 변경
        userMission.updateStatus(UserMissionStatus.PENDING);

        try {
            Post saved = postRepository.save(post);
            log.info("인증 게시글 생성 완료 - postId={}, postType={}, userMissionId={}, userId={}, status={}", 
                    saved.getId(), saved.getPostType(), userMission.getId(), userId, saved.getStatus());
            
            // QueryDSL로 fetch join하여 user, userMission, mission 정보를 모두 로드
            Post verifiedPost = postRepository.getPostByIdExcludingDeleted(saved.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
            
            // 좋아요 수와 댓글 수 조회
            long likeCount = postLikeRepository.countByPostId(verifiedPost.getId());
            long commentCount = commentRepository.countByPostId(verifiedPost.getId());
            
            return PostResponse.from(verifiedPost, commentCount, likeCount, false, userId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // UNIQUE 제약조건 위반 시 (동시 요청으로 인한 중복 생성 시도)
            log.warn("인증글 중복 생성 시도 감지 - userMissionId={}, userId={}", userMission.getId(), userId);
            // 기존 게시글 조회하여 반환 (QueryDSL로 fetch join)
            Post existingPost = postRepository.findByUserMissionId(userMission.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_ALREADY_EXISTS));
            
            // 좋아요 수와 댓글 수 조회
            long likeCount = postLikeRepository.countByPostId(existingPost.getId());
            long commentCount = commentRepository.countByPostId(existingPost.getId());
            
            return PostResponse.from(existingPost, commentCount, likeCount, false, userId);
        }
    }

    /**
     * 인증 게시글 목록 조회 (VERIFICATION 타입만)
     */
    public Page<PostResponse> getVerificationPosts(String status, Pageable pageable, Long currentUserId) {
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        
        // 정렬 필드 검증
        Pageable validatedPageable = validateAndSanitizePageable(pageable);

        return postRepository.findVerificationPostsWithFilters(status, validatedPageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    long likeCount = postLikeRepository.countByPostId(post.getId());
                    boolean isLiked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);
                    PostResponse response = PostResponse.from(post, commentCount, likeCount, isLiked, currentUserId);
                    // 디버깅: title이 비어있는 경우 로그
                    if (post.isVerificationPost() && (response.getTitle() == null || response.getTitle().isEmpty())) {
                        log.warn("인증글 title 누락 - postId={}, dbTitle={}, missionTitle={}", 
                                post.getId(), post.getTitle(), 
                                post.getUserMission() != null && post.getUserMission().getMission() != null 
                                        ? post.getUserMission().getMission().getTitle() : "null");
                    }
                    return response;
                });
    }

    /**
     * 인증 게시글 상세 조회
     */
    public PostResponse getVerificationPost(Long postId, Long currentUserId) {
        Post post = postRepository.findVerificationPostById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        long commentCount = commentRepository.countByPostId(postId);
        long likeCount = postLikeRepository.countByPostId(postId);
        boolean isLiked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);

        return PostResponse.from(post, commentCount, likeCount, isLiked, currentUserId);
    }

    /**
     * 인증 게시글 수정
     */
    @Transactional
    public PostResponse updateVerificationPost(Long postId, Long userId, VerificationPostRequest request) {
        Post post = postRepository.findVerificationPostById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.isVerificationPost()) {
            throw new CustomException(ErrorCode.INVALID_POST_TYPE);
        }

        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        if ("APPROVED".equals(post.getStatus())) {
            throw new CustomException(ErrorCode.VERIFICATION_ALREADY_APPROVED);
        }

        // 비속어 필터링
        validateBadWords(null, request.getContent());

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_DATA);
            }
        }

        post.updateVerificationContent(request.getContent(), imageUrlsJson, request.getCompletionRate());

        long commentCount = commentRepository.countByPostId(postId);
        long likeCount = postLikeRepository.countByPostId(postId);
        boolean isLiked = postLikeRepository.existsByPostAndUser(post, userRepository.findById(userId).orElse(null));

        return PostResponse.from(post, commentCount, likeCount, isLiked, userId);
    }

    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostRequest request) {
        Post post = findPostById(postId);

        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // 비속어 필터링
        validateBadWords(request.getTitle(), request.getContent());

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
        return PostResponse.from(post, commentCount, likeCount, false, userId);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = findPostById(postId);

        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // 인증글인 경우 UserMission 상태를 되돌림
        if (post.isVerificationPost() && post.getUserMission() != null) {
            UserMission userMission = post.getUserMission();
            // PENDING 상태였으면 ASSIGNED로 되돌림
            if (userMission.getStatus() == UserMissionStatus.PENDING) {
                userMission.updateStatus(UserMissionStatus.ASSIGNED);
                log.info("인증글 삭제로 인해 UserMission 상태 복원: userMissionId={}, status={}", 
                        userMission.getId(), userMission.getStatus());
            }
        }

        post.softDelete();
    }

    // ========================================
    // 댓글 CRUD
    // ========================================

    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        return getComments(postId, pageable, null);
    }

    public Page<CommentResponse> getComments(Long postId, Pageable pageable, Long currentUserId) {
        findPostById(postId);
        List<Comment> comments = commentRepository.findParentCommentsByPostIdWithUser(postId);
        List<CommentResponse> responseList = comments.stream()
                .map(comment -> CommentResponse.fromWithReplies(comment, currentUserId))
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

        // 비속어 필터링
        validateBadWords(null, request.getContent());

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

        return CommentResponse.from(saved, userId);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest request) {
        Comment comment = findCommentById(commentId);

        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        // 비속어 필터링
        validateBadWords(null, request.getContent());

        comment.updateContent(request.getContent());
        return CommentResponse.from(comment, userId);
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
            postLikeRepository.saveAndFlush(newLike); // 즉시 DB에 반영
            isLiked = true;
            log.info("좋아요 추가 - postId={}, userId={}", postId, userId);

            // 좋아요 알림 (임시 비활성화 - notification 테이블 마이그레이션 필요)
            // sendLikeNotification(post.getUser(), user, post);
        }

        // VERIFICATION 타입: 좋아요 추가/취소 후 인증 체크 (좋아요 수가 변경되었으므로 재확인)
        if (post.isVerificationPost()) {
            // flush 후 다시 조회하여 최신 좋아요 수 확인
            long likeCount = postLikeRepository.countByPostId(postId);
            newlyVerified = post.checkAndApproveByLikes(likeCount);

            if (newlyVerified) {
                // Post 엔티티의 status 변경사항을 DB에 저장
                postRepository.saveAndFlush(post);
                // 저장 후 최신 상태로 다시 조회 (1차 캐시 문제 방지)
                post = postRepository.findByIdAndDelFlagFalse(postId)
                        .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
                log.info("인증 완료! postId={}, likeCount={}, status={}", postId, likeCount, post.getStatus());
                
                // 인증 완료 처리 (뱃지, 경험치 등)
                // UserMission이 연결되어 있는 경우에만 완료 처리
                if (post.getUserMission() != null) {
                    userMissionService.completeMissionVerification(post.getUserMission());
                    log.info("UserMission 완료 처리: userMissionId={}, userId={}", 
                            post.getUserMission().getId(), post.getUser().getId());
                } else {
                    log.warn("인증 게시글에 UserMission이 연결되어 있지 않습니다. postId={}", postId);
                }

                // 알림 전송
                sendVerificationSuccessNotification(post.getUser(), post);
            }
        }

        long likeCount = postLikeRepository.countByPostId(postId);

        result.put("isLiked", isLiked);
        result.put("likeCount", likeCount);
        result.put("verified", newlyVerified);

        return result;
    }

    /**
     * 인증 게시글을 직접 인증 처리 (관리자용 또는 자동 처리용)
     * Post를 APPROVED로 변경하고, 연결된 UserMission도 COMPLETED로 변경
     * 
     * @param postId 인증 게시글 ID
     * @param userId 요청한 사용자 ID (권한 확인용, null 가능)
     * @return 인증 처리 성공 여부
     */
    @Transactional
    public boolean approveVerificationPost(Long postId, Long userId) {
        Post post = postRepository.findVerificationPostById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.isVerificationPost()) {
            throw new CustomException(ErrorCode.INVALID_POST_TYPE);
        }

        // 이미 인증 완료된 경우
        if ("APPROVED".equals(post.getStatus())) {
            log.info("인증 게시글이 이미 APPROVED 상태입니다. postId={}", postId);
            return false;
        }

        // Post를 APPROVED로 변경 (좋아요 수와 관계없이 강제 인증)
        boolean newlyApproved = post.approve();
        if (!newlyApproved) {
            log.info("인증 게시글이 이미 APPROVED 상태입니다. postId={}", postId);
            return false;
        }
        postRepository.saveAndFlush(post);

        // 저장 후 최신 상태로 다시 조회 (1차 캐시 문제 방지)
        post = postRepository.findByIdAndDelFlagFalse(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        log.info("인증 게시글 직접 인증 처리: postId={}, status={}", postId, post.getStatus());

        // 연결된 UserMission이 있으면 완료 처리
        if (post.getUserMission() != null) {
            UserMission userMission = post.getUserMission();
            // 이미 COMPLETED 상태가 아니면 완료 처리
            if (userMission.getStatus() != UserMissionStatus.COMPLETED) {
                userMissionService.completeMissionVerification(userMission);
                log.info("UserMission 완료 처리: userMissionId={}, status={}", 
                        userMission.getId(), userMission.getStatus());
            }
        }

        // 알림 전송
        sendVerificationSuccessNotification(post.getUser(), post);

        return true;
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
        return postRepository.findByIdAndDelFlagFalse(postId)
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

    /**
     * 비속어 필터링 검사
     */
    private void validateBadWords(String title, String content) {
        if (title != null && badWordFilterService.containsBadWordIgnoreBlank(title)) {
            throw new CustomException(ErrorCode.BAD_WORD_DETECTED);
        }
        if (content != null && badWordFilterService.containsBadWordIgnoreBlank(content)) {
            throw new CustomException(ErrorCode.BAD_WORD_DETECTED);
        }
    }

    /**
     * Pageable의 Sort를 검증하고 허용된 필드만 사용하도록 필터링
     */
    private Pageable validateAndSanitizePageable(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return pageable;
        }

        List<Sort.Order> validOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (ALLOWED_SORT_FIELDS.contains(property)) {
                validOrders.add(order);
            } else {
                log.warn("허용되지 않은 정렬 필드: {}. 기본 정렬(createdAt DESC)을 사용합니다.", property);
            }
        }

        // 유효한 정렬이 없으면 기본 정렬 사용
        if (validOrders.isEmpty()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(validOrders)
        );
    }
}
