package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 게시글 Repository
 * - GENERAL: 일반 게시글
 * - VERIFICATION: 인증 게시글 (미션 정보는 userMission에서 참조)
 * 
 * QueryDSL을 사용한 복잡한 쿼리는 PostRepositoryCustom을 통해 구현
 */
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

      // ========================================
      // 게시글 목록 조회 (통합)
      // ========================================
      // QueryDSL로 구현됨 (PostRepositoryCustom 참조)

      // ========================================
      // 타입별 조회
      // ========================================
      // QueryDSL로 구현됨 (PostRepositoryCustom 참조)

      // ========================================
      // 인증글 조회
      // ========================================
      // QueryDSL로 구현됨 (PostRepositoryCustom 참조)

      // ========================================
      // 단건 조회
      // ========================================
      // QueryDSL로 구현됨 (PostRepositoryCustom 참조)

      // ========================================
      // 통계
      // ========================================
      // QueryDSL로 구현됨 (PostRepositoryCustom 참조)
}
