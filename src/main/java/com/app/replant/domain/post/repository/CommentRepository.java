package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Comment Repository
 * QueryDSL을 사용한 복잡한 쿼리는 CommentRepositoryCustom을 통해 구현
 */
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {
}
