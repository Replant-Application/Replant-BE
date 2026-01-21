package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * PostRepository Custom Interface
 * QueryDSL을 사용한 복잡한 쿼리 메서드 정의
 */
public interface PostRepositoryCustom {

    // ========================================
    // 게시글 목록 조회 (통합)
    // ========================================

    /**
     * 모든 게시글 조회 (GENERAL + VERIFICATION)
     */
    Page<Post> findAllPosts(Pageable pageable);

    /**
     * 게시글 목록 조회 (하위 호환성 - 파라미터 무시)
     */
    Page<Post> findWithFilters(
            Long missionId,
            boolean badgeOnly,
            Pageable pageable);

    /**
     * 커뮤니티 게시글 조회 (하위 호환성)
     */
    Page<Post> findCommunityPostsWithFilters(
            Long missionId,
            boolean badgeOnly,
            Pageable pageable);

    // ========================================
    // 타입별 조회
    // ========================================

    Page<Post> findByPostType(PostType postType, Pageable pageable);

    Page<Post> findByUserIdAndPostType(Long userId, PostType postType, Pageable pageable);

    // ========================================
    // 인증글 조회
    // ========================================

    Page<Post> findVerificationPostsWithFilters(String status, Pageable pageable);

    Optional<Post> findVerificationPostById(Long postId);

    Optional<Post> findByUserMissionId(Long userMissionId);

    // ========================================
    // 단건 조회
    // ========================================

    Optional<Post> getPostByIdExcludingDeleted(Long postId);

    Optional<Post> findByIdAndUserId(Long postId, Long userId);

    /**
     * 사용자별 삭제되지 않은 게시글 조회
     * 통합된 메서드: getUserPostsExcludingDeleted와 동일한 기능
     */
    Page<Post> findByUserIdAndNotDeleted(Long userId, Pageable pageable);

    // ========================================
    // 통계
    // ========================================

    long countApprovedVerificationsByUserId(Long userId);

    long countByUserId(Long userId);
}
