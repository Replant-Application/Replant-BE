package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.enums.PostType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.app.replant.domain.post.entity.QPost.post;
import static com.app.replant.domain.user.entity.QUser.user;
import static com.app.replant.domain.usermission.entity.QUserMission.userMission;
import static com.app.replant.domain.mission.entity.QMission.mission;

/**
 * PostRepository Custom Implementation
 * QueryDSL을 사용한 복잡한 쿼리 구현
 */
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // ========================================
    // 공통 조건
    // ========================================

    private BooleanExpression isNotDeleted() {
        return post.delFlag.isFalse().or(post.delFlag.isNull());
    }

    private BooleanExpression isPostType(PostType postType) {
        return post.postType.eq(postType);
    }

    private BooleanExpression isVerificationType() {
        return post.postType.eq(PostType.VERIFICATION);
    }

    private BooleanExpression isGeneralOrVerification() {
        return post.postType.eq(PostType.GENERAL).or(post.postType.eq(PostType.VERIFICATION));
    }

    // ========================================
    // 게시글 목록 조회 (통합)
    // ========================================

    @Override
    public Page<Post> findAllPosts(Pageable pageable) {
        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .join(post.user, user).fetchJoin()
                .leftJoin(post.userMission, userMission).fetchJoin()
                .leftJoin(userMission.mission, mission).fetchJoin()
                .where(isGeneralOrVerification().and(isNotDeleted()))
                .orderBy(post.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public Page<Post> findWithFilters(
            Long missionId,
            Long customMissionId,
            boolean badgeOnly,
            Pageable pageable) {
        
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(isGeneralOrVerification());
        builder.and(isNotDeleted());

        // 파라미터는 현재 무시 (하위 호환성)
        // 필요시 아래 주석 해제하여 사용
        // if (missionId != null) {
        //     builder.and(post.userMission.mission.id.eq(missionId));
        // }
        // if (badgeOnly) {
        //     builder.and(post.hasValidBadge.isTrue());
        // }

        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .join(post.user, user).fetchJoin()
                .leftJoin(post.userMission, userMission).fetchJoin()
                .leftJoin(userMission.mission, mission).fetchJoin()
                .where(builder)
                .orderBy(post.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public Page<Post> findCommunityPostsWithFilters(
            Long missionId,
            Long customMissionId,
            boolean badgeOnly,
            Pageable pageable) {
        // findWithFilters와 동일한 구현
        return findWithFilters(missionId, customMissionId, badgeOnly, pageable);
    }

    // ========================================
    // 타입별 조회
    // ========================================

    @Override
    public Page<Post> findByPostType(PostType postType, Pageable pageable) {
        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .where(isPostType(postType).and(isNotDeleted()))
                .orderBy(post.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public Page<Post> findByUserIdAndPostType(Long userId, PostType postType, Pageable pageable) {
        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .where(post.user.id.eq(userId)
                        .and(isPostType(postType))
                        .and(isNotDeleted()))
                .orderBy(post.createdAt.desc());

        return getPage(query, pageable);
    }

    // ========================================
    // 인증글 조회
    // ========================================

    @Override
    public Page<Post> findVerificationPostsWithFilters(String status, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(isVerificationType());
        builder.and(isNotDeleted());

        if (status != null) {
            builder.and(post.status.eq(status));
        }

        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .join(post.user, user).fetchJoin()
                .leftJoin(post.userMission, userMission).fetchJoin()
                .leftJoin(userMission.mission, mission).fetchJoin()
                .where(builder)
                .orderBy(post.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public Optional<Post> findVerificationPostById(Long postId) {
        Post result = queryFactory
                .selectFrom(post)
                .join(post.user, user).fetchJoin()
                .leftJoin(post.userMission, userMission).fetchJoin()
                .leftJoin(userMission.mission, mission).fetchJoin()
                .where(post.id.eq(postId)
                        .and(isVerificationType())
                        .and(isNotDeleted()))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Post> findByUserMissionId(Long userMissionId) {
        Post result = queryFactory
                .selectFrom(post)
                .where(post.userMission.id.eq(userMissionId)
                        .and(isVerificationType())
                        .and(isNotDeleted()))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    // ========================================
    // 단건 조회
    // ========================================

    @Override
    public Optional<Post> findByIdAndNotDeleted(Long postId) {
        Post result = queryFactory
                .selectFrom(post)
                .join(post.user, user).fetchJoin()
                .leftJoin(post.userMission, userMission).fetchJoin()
                .leftJoin(userMission.mission, mission).fetchJoin()
                .where(post.id.eq(postId).and(isNotDeleted()))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Post> findByIdAndUserId(Long postId, Long userId) {
        Post result = queryFactory
                .selectFrom(post)
                .where(post.id.eq(postId)
                        .and(post.user.id.eq(userId))
                        .and(isNotDeleted()))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<Post> findByUserIdAndNotDeleted(Long userId, Pageable pageable) {
        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .where(post.user.id.eq(userId).and(isNotDeleted()))
                .orderBy(post.createdAt.desc());

        return getPage(query, pageable);
    }

    // ========================================
    // 통계
    // ========================================

    @Override
    public long countApprovedVerificationsByUserId(Long userId) {
        return queryFactory
                .select(post.count())
                .from(post)
                .where(post.user.id.eq(userId)
                        .and(isVerificationType())
                        .and(post.status.eq("APPROVED")))
                .fetchOne();
    }

    @Override
    public long countByUserId(Long userId) {
        return queryFactory
                .select(post.count())
                .from(post)
                .where(post.user.id.eq(userId).and(isNotDeleted()))
                .fetchOne();
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    /**
     * QueryDSL 쿼리를 Page로 변환
     */
    private Page<Post> getPage(JPAQuery<Post> query, Pageable pageable) {
        // 정렬 적용
        if (pageable.getSort().isSorted()) {
            List<com.querydsl.core.types.OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                com.querydsl.core.types.OrderSpecifier<?> orderSpec = getOrderSpecifier(order.getProperty(), order.isAscending());
                if (orderSpec != null) {
                    orderSpecifiers.add(orderSpec);
                }
            });
            if (!orderSpecifiers.isEmpty()) {
                query.orderBy(orderSpecifiers.toArray(new com.querydsl.core.types.OrderSpecifier[0]));
            } else {
                // 기본 정렬: createdAt DESC
                query.orderBy(post.createdAt.desc());
            }
        } else {
            // 기본 정렬: createdAt DESC
            query.orderBy(post.createdAt.desc());
        }

        // 페이징 적용
        JPAQuery<Post> pagedQuery = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // Count 쿼리 - JOIN 없이 where 조건만 복사
        // QueryDSL 5.0에서는 fetchCount()가 deprecated되어 count() 쿼리로 변경
        com.querydsl.core.types.Predicate whereCondition = query.getMetadata().getWhere();
        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
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

    /**
     * 문자열 필드명과 정렬 방향으로 OrderSpecifier 생성
     * @return OrderSpecifier 또는 null (알 수 없는 필드인 경우)
     */
    private com.querydsl.core.types.OrderSpecifier<?> getOrderSpecifier(String property, boolean ascending) {
        com.querydsl.core.types.Order direction = ascending 
                ? com.querydsl.core.types.Order.ASC 
                : com.querydsl.core.types.Order.DESC;
        
        switch (property) {
            case "id":
                return new com.querydsl.core.types.OrderSpecifier<>(direction, post.id);
            case "createdAt":
                return new com.querydsl.core.types.OrderSpecifier<>(direction, post.createdAt);
            case "updatedAt":
                return new com.querydsl.core.types.OrderSpecifier<>(direction, post.updatedAt);
            case "status":
                return new com.querydsl.core.types.OrderSpecifier<>(direction, post.status);
            case "verifiedAt":
                return new com.querydsl.core.types.OrderSpecifier<>(direction, post.verifiedAt);
            default:
                // 알 수 없는 필드는 null 반환 (기본 정렬 사용)
                return null;
        }
    }
}
