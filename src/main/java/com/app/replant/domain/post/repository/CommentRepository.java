package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    Page<Comment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    // 최상위 댓글만 조회 (parent가 null인 것들)
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.replies WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    Page<Comment> findParentCommentsByPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.id = :commentId AND c.user.id = :userId")
    Optional<Comment> findByIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    // VerificationPost 댓글 조회
    @Query("SELECT c FROM Comment c WHERE c.verificationPost.id = :verificationPostId")
    Page<Comment> findByVerificationPostId(@Param("verificationPostId") Long verificationPostId, Pageable pageable);

    // VerificationPost 최상위 댓글만 조회
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.replies WHERE c.verificationPost.id = :verificationPostId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    Page<Comment> findParentCommentsByVerificationPostId(@Param("verificationPostId") Long verificationPostId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.verificationPost.id = :verificationPostId")
    long countByVerificationPostId(@Param("verificationPostId") Long verificationPostId);
}
