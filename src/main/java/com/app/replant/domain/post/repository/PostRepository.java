package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.enums.PostType;
import com.app.replant.domain.verification.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 게시글 Repository (단순화)
 * - GENERAL: 일반 게시글
 * - VERIFICATION: 인증 게시글 (미션 정보는 userMission에서 참조)
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    // ========================================
    // 게시글 목록 조회 (통합)
    // ========================================

    /**
     * 모든 게시글 조회 (GENERAL + VERIFICATION)
     */
    @Query("SELECT p FROM Post p WHERE " +
           "(p.postType = 'GENERAL' OR p.postType = 'VERIFICATION') AND " +
           "(p.delFlag = false OR p.delFlag IS NULL) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findAllPosts(Pageable pageable);

    /**
     * 게시글 목록 조회 (하위 호환성 - 파라미터 무시)
     */
    @Query("SELECT p FROM Post p WHERE " +
           "(p.postType = 'GENERAL' OR p.postType = 'VERIFICATION') AND " +
           "(p.delFlag = false OR p.delFlag IS NULL) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findWithFilters(
        @Param("missionId") Long missionId,
        @Param("customMissionId") Long customMissionId,
        @Param("badgeOnly") boolean badgeOnly,
        Pageable pageable
    );

    /**
     * 커뮤니티 게시글 조회 (하위 호환성)
     */
    @Query("SELECT p FROM Post p WHERE " +
           "(p.postType = 'GENERAL' OR p.postType = 'VERIFICATION') AND " +
           "(p.delFlag = false OR p.delFlag IS NULL) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findCommunityPostsWithFilters(
        @Param("missionId") Long missionId,
        @Param("customMissionId") Long customMissionId,
        @Param("badgeOnly") boolean badgeOnly,
        Pageable pageable
    );

    // ========================================
    // 타입별 조회
    // ========================================

    @Query("SELECT p FROM Post p WHERE p.postType = :postType AND (p.delFlag = false OR p.delFlag IS NULL) ORDER BY p.createdAt DESC")
    Page<Post> findByPostType(@Param("postType") PostType postType, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId AND p.postType = :postType AND (p.delFlag = false OR p.delFlag IS NULL) ORDER BY p.createdAt DESC")
    Page<Post> findByUserIdAndPostType(@Param("userId") Long userId, @Param("postType") PostType postType, Pageable pageable);

    // ========================================
    // 인증글 조회
    // ========================================

    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.userMission um " +
           "LEFT JOIN FETCH um.mission " +
           "" +
           "WHERE p.postType = 'VERIFICATION' " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (p.delFlag = false OR p.delFlag IS NULL) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findVerificationPostsWithFilters(
        @Param("status") VerificationStatus status,
        Pageable pageable
    );

    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.userMission um " +
           "LEFT JOIN FETCH um.mission " +
           "" +
           "WHERE p.id = :postId " +
           "AND p.postType = 'VERIFICATION' " +
           "AND (p.delFlag = false OR p.delFlag IS NULL)")
    Optional<Post> findVerificationPostById(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p WHERE p.userMission.id = :userMissionId AND p.postType = 'VERIFICATION' AND (p.delFlag = false OR p.delFlag IS NULL)")
    Optional<Post> findByUserMissionId(@Param("userMissionId") Long userMissionId);

    // ========================================
    // 단건 조회
    // ========================================

    @Query("SELECT p FROM Post p WHERE p.id = :postId AND (p.delFlag = false OR p.delFlag IS NULL)")
    Optional<Post> findByIdAndNotDeleted(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.user.id = :userId AND (p.delFlag = false OR p.delFlag IS NULL)")
    Optional<Post> findByIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId AND (p.delFlag = false OR p.delFlag IS NULL) ORDER BY p.createdAt DESC")
    Page<Post> findByUserIdAndNotDeleted(@Param("userId") Long userId, Pageable pageable);

    // ========================================
    // 통계
    // ========================================

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.postType = 'VERIFICATION' AND p.status = 'APPROVED'")
    long countApprovedVerificationsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND (p.delFlag = false OR p.delFlag IS NULL)")
    long countByUserId(@Param("userId") Long userId);
}
