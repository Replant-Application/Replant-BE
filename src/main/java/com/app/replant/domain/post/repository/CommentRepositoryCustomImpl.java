package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Comment;
import com.app.replant.domain.post.enums.CommentTargetType;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

import static com.app.replant.domain.post.entity.QComment.comment;
import static com.app.replant.domain.user.entity.QUser.user;

/**
 * CommentRepository Custom Implementation
 * QueryDSL을 사용한 복잡한 쿼리 구현
 */
@RequiredArgsConstructor
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Comment> findByPostId(Long postId, Pageable pageable) {
        JPAQuery<Comment> query = queryFactory
                .selectFrom(comment)
                .where(comment.post.id.eq(postId))
                .orderBy(comment.createdAt.asc());

        return getPage(query, pageable);
    }

    @Override
    public List<Comment> findParentCommentsByPostIdWithUser(Long postId) {
        // replies는 OneToMany이므로 별도로 조회해야 함
        // 일단 기본적인 fetch join만 사용
        return queryFactory
                .selectFrom(comment)
                .leftJoin(comment.user, user).fetchJoin()
                .leftJoin(comment.replies).fetchJoin()
                .where(comment.post.id.eq(postId)
                        .and(comment.parent.isNull()))
                .distinct()
                .orderBy(comment.createdAt.asc())
                .fetch();
    }

    @Override
    public List<Comment> findAllByPostIdWithUser(Long postId) {
        return queryFactory
                .selectFrom(comment)
                .leftJoin(comment.user, user).fetchJoin()
                .where(comment.post.id.eq(postId))
                .orderBy(comment.createdAt.asc())
                .fetch();
    }

    @Override
    public Optional<Comment> findByIdAndUserId(Long commentId, Long userId) {
        Comment result = queryFactory
                .selectFrom(comment)
                .where(comment.id.eq(commentId)
                        .and(comment.user.id.eq(userId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public long countByPostId(Long postId) {
        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public List<Comment> findRepliesByParentId(Long parentId) {
        return queryFactory
                .selectFrom(comment)
                .where(comment.parent.id.eq(parentId))
                .orderBy(comment.createdAt.asc())
                .fetch();
    }

    @Override
    public Page<Comment> findByTarget(CommentTargetType targetType, Long targetId, Pageable pageable) {
        JPAQuery<Comment> query = queryFactory
                .selectFrom(comment)
                .where(comment.targetType.eq(targetType)
                        .and(comment.targetId.eq(targetId)))
                .orderBy(comment.createdAt.asc());

        return getPage(query, pageable);
    }

    @Override
    public List<Comment> findParentCommentsByTargetWithUser(CommentTargetType targetType, Long targetId) {
        // replies는 OneToMany이므로 별도로 조회해야 함
        // 일단 기본적인 fetch join만 사용
        return queryFactory
                .selectFrom(comment)
                .leftJoin(comment.user, user).fetchJoin()
                .leftJoin(comment.replies).fetchJoin()
                .where(comment.targetType.eq(targetType)
                        .and(comment.targetId.eq(targetId))
                        .and(comment.parent.isNull()))
                .distinct()
                .orderBy(comment.createdAt.asc())
                .fetch();
    }

    @Override
    public long countByTarget(CommentTargetType targetType, Long targetId) {
        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.targetType.eq(targetType)
                        .and(comment.targetId.eq(targetId)))
                .fetchOne();

        return count != null ? count : 0L;
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    /**
     * QueryDSL 쿼리를 Page로 변환
     */
    private Page<Comment> getPage(JPAQuery<Comment> query, Pageable pageable) {
        // 페이징 적용
        JPAQuery<Comment> pagedQuery = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // Count 쿼리 - JOIN 없이 where 조건만 복사
        com.querydsl.core.types.Predicate whereCondition = query.getMetadata().getWhere();
        JPAQuery<Long> countQuery = queryFactory
                .select(comment.count())
                .from(comment)
                .where(whereCondition);

        return PageableExecutionUtils.getPage(
                pagedQuery.fetch(),
                pageable,
                () -> {
                    Long count = countQuery.fetchOne();
                    return count != null ? count : 0L;
                }
        );
    }
}
