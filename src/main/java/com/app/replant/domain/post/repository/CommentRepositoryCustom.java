package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Comment;
import com.app.replant.domain.post.enums.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * CommentRepository Custom Interface
 * QueryDSL을 사용한 복잡한 쿼리 메서드 정의
 */
public interface CommentRepositoryCustom {

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    List<Comment> findParentCommentsByPostIdWithUser(Long postId);

    Optional<Comment> findByIdAndUserId(Long commentId, Long userId);

    long countByPostId(Long postId);

    List<Comment> findRepliesByParentId(Long parentId);

    Page<Comment> findByTarget(CommentTargetType targetType, Long targetId, Pageable pageable);

    List<Comment> findParentCommentsByTargetWithUser(CommentTargetType targetType, Long targetId);

    long countByTarget(CommentTargetType targetType, Long targetId);
}
