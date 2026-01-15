package com.app.replant.domain.usermission.repository;

import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.app.replant.domain.usermission.entity.QUserMission.userMission;
import static com.app.replant.domain.user.entity.QUser.user;
import static com.app.replant.domain.mission.entity.QMission.mission;

/**
 * UserMissionRepository Custom Implementation
 * QueryDSL을 사용한 복잡한 쿼리 구현
 */
@RequiredArgsConstructor
public class UserMissionRepositoryCustomImpl implements UserMissionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<UserMission> findByUserIdWithFilters(Long userId, Pageable pageable) {
        JPAQuery<UserMission> query = queryFactory
                .selectFrom(userMission)
                .where(userMission.user.id.eq(userId)
                        .and(userMission.status.eq(UserMissionStatus.ASSIGNED)
                                .or(userMission.status.eq(UserMissionStatus.PENDING))))
                .orderBy(userMission.assignedAt.desc(), userMission.id.desc());

        return getPage(query, pageable);
    }

    @Override
    public Optional<UserMission> findByIdAndUserId(Long userMissionId, Long userId) {
        UserMission result = queryFactory
                .selectFrom(userMission)
                .where(userMission.id.eq(userMissionId)
                        .and(userMission.user.id.eq(userId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public long countByUserIdAndStatus(Long userId, UserMissionStatus status) {
        Long count = queryFactory
                .select(userMission.count())
                .from(userMission)
                .where(userMission.user.id.eq(userId)
                        .and(userMission.status.eq(status)))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public List<UserMission> findRecentCompletedByMissionExcludingUser(
            Long missionId,
            Long excludeUserId,
            Pageable pageable) {
        return queryFactory
                .selectFrom(userMission)
                .where(userMission.mission.id.eq(missionId)
                        .and(userMission.status.eq(UserMissionStatus.COMPLETED))
                        .and(userMission.user.id.ne(excludeUserId)))
                .orderBy(userMission.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<UserMission> findRecentCompletedByCustomMissionExcludingUser(
            Long customMissionId,
            Long excludeUserId,
            Pageable pageable) {
        return queryFactory
                .selectFrom(userMission)
                .where(userMission.mission.id.eq(customMissionId)
                        .and(userMission.missionType.eq(MissionType.CUSTOM))
                        .and(userMission.status.eq(UserMissionStatus.COMPLETED))
                        .and(userMission.user.id.ne(excludeUserId)))
                .orderBy(userMission.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public Page<UserMission> findMissionHistoryByUserId(Long userId, Pageable pageable) {
        JPAQuery<UserMission> query = queryFactory
                .selectFrom(userMission)
                .where(userMission.user.id.eq(userId)
                        .and(userMission.status.eq(UserMissionStatus.COMPLETED)))
                .orderBy(userMission.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public List<UserMission> findByUserIdAndMissionIdAndStatusAssigned(
            Long userId,
            Long missionId) {
        return queryFactory
                .selectFrom(userMission)
                .where(userMission.user.id.eq(userId)
                        .and(userMission.mission.id.eq(missionId))
                        .and(userMission.status.eq(UserMissionStatus.ASSIGNED)))
                .orderBy(userMission.id.desc())
                .fetch();
    }

    @Override
    public List<UserMission> findByUserIdAndMissionId(
            Long userId,
            Long missionId) {
        return queryFactory
                .selectFrom(userMission)
                .join(userMission.user, user).fetchJoin()
                .join(userMission.mission, mission).fetchJoin()
                .where(userMission.user.id.eq(userId)
                        .and(userMission.mission.id.eq(missionId)))
                .distinct()
                .orderBy(userMission.id.desc())
                .fetch();
    }

    @Override
    public List<UserMission> findExpiredMissions(LocalDateTime now) {
        return queryFactory
                .selectFrom(userMission)
                .where(userMission.status.eq(UserMissionStatus.ASSIGNED)
                        .and(userMission.dueDate.lt(now)))
                .fetch();
    }

    @Override
    public List<UserMission> findByUserIdAndMissionIds(
            Long userId,
            List<Long> missionIds) {
        return queryFactory
                .selectFrom(userMission)
                .join(userMission.user, user).fetchJoin()
                .join(userMission.mission, mission).fetchJoin()
                .where(userMission.user.id.eq(userId)
                        .and(userMission.mission.id.in(missionIds)))
                .distinct()
                .orderBy(userMission.mission.id.asc(), userMission.id.desc())
                .fetch();
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    /**
     * QueryDSL 쿼리를 Page로 변환
     */
    private Page<UserMission> getPage(JPAQuery<UserMission> query, Pageable pageable) {
        // 정렬 적용 (쿼리에 이미 orderBy가 있으면 추가 정렬 안 함)
        if (pageable.getSort().isSorted() && query.getMetadata().getOrderBy().isEmpty()) {
            pageable.getSort().forEach(order -> {
                // 간단한 필드명으로만 정렬 지원 (복잡한 정렬은 쿼리에서 처리)
            });
        }

        // 페이징 적용
        JPAQuery<UserMission> pagedQuery = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // Count 쿼리 - JOIN 없이 where 조건만 복사
        com.querydsl.core.types.Predicate whereCondition = query.getMetadata().getWhere();
        JPAQuery<Long> countQuery = queryFactory
                .select(userMission.count())
                .from(userMission)
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
