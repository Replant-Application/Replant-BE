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

import java.time.LocalDate;
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

    /**
     * 사용자별 미션 목록 조회 - 오늘 할당된 미션만 반환
     * 
     * 투두리스트 개념을 유지하기 위해 오늘 날짜에 할당된 미션만 조회합니다.
     * 전날 할당된 미션은 완료 여부와 관계없이 제외되어 다음날 조회되지 않습니다.
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 오늘 날짜에 할당된 미션 목록 (ASSIGNED, PENDING, COMPLETED 상태 모두 포함)
     */
    @Override
    public Page<UserMission> findByUserIdWithFilters(Long userId, Pageable pageable) {
        // 오늘 날짜 범위 계산 (00:00:00 ~ 23:59:59)
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();      // 오늘 00:00:00
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();  // 내일 00:00:00 (오늘 23:59:59까지)
        
        JPAQuery<UserMission> query = queryFactory
                .selectFrom(userMission)
                .where(userMission.user.id.eq(userId)
                        // 오늘 할당된 미션만 조회 (assignedAt이 오늘 날짜 범위 내)
                        .and(userMission.assignedAt.goe(startOfToday))
                        .and(userMission.assignedAt.lt(endOfToday))
                        // 상태 필터: ASSIGNED, PENDING, COMPLETED 모두 포함
                        .and(
                                userMission.status.eq(UserMissionStatus.ASSIGNED)
                                        .or(userMission.status.eq(UserMissionStatus.PENDING))
                                        .or(userMission.status.eq(UserMissionStatus.COMPLETED))
                        ))
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
        // 오늘 날짜 범위 계산 (00:00:00 ~ 23:59:59)
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();      // 오늘 00:00:00
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();  // 내일 00:00:00 (오늘 23:59:59까지)
        
        // 조건 분리: 오늘 할당된 미션이거나 PENDING 상태인 미션
        // PENDING 상태인 미션은 인증 게시글을 올린 경우이므로 날짜와 관계없이 조회
        com.querydsl.core.types.dsl.BooleanExpression todayAssignedCondition = 
                userMission.assignedAt.goe(startOfToday)
                        .and(userMission.assignedAt.lt(endOfToday));
        com.querydsl.core.types.dsl.BooleanExpression pendingStatusCondition = 
                userMission.status.eq(UserMissionStatus.PENDING);
        
        return queryFactory
                .selectFrom(userMission)
                .join(userMission.user, user).fetchJoin()
                .join(userMission.mission, mission).fetchJoin()
                .where(userMission.user.id.eq(userId)
                        .and(userMission.mission.id.in(missionIds))
                        // 오늘 할당된 미션이거나 PENDING 상태인 미션
                        .and(todayAssignedCondition.or(pendingStatusCondition)))
                .distinct()
                .orderBy(userMission.mission.id.asc(), userMission.id.desc())
                .fetch();
    }

    @Override
    public List<UserMission> findByUserIdAndAssignedDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        return queryFactory
                .selectFrom(userMission)
                .join(userMission.user, user).fetchJoin()
                .join(userMission.mission, mission).fetchJoin()
                .where(userMission.user.id.eq(userId)
                        .and(userMission.assignedAt.goe(startOfDay))
                        .and(userMission.assignedAt.lt(endOfDay)))
                .distinct()
                .orderBy(userMission.assignedAt.desc(), userMission.id.desc())
                .fetch();
    }

    @Override
    public List<UserMission> findByUserIdAndAssignedDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startOfStartDate = startDate.atStartOfDay();
        LocalDateTime endOfEndDate = endDate.plusDays(1).atStartOfDay();
        
        return queryFactory
                .selectFrom(userMission)
                .join(userMission.user, user).fetchJoin()
                .join(userMission.mission, mission).fetchJoin()
                .where(userMission.user.id.eq(userId)
                        .and(userMission.assignedAt.goe(startOfStartDate))
                        .and(userMission.assignedAt.lt(endOfEndDate)))
                .distinct()
                .orderBy(userMission.assignedAt.desc(), userMission.id.desc())
                .fetch();
    }

    @Override
    public long countDistinctUsersByMissionId(Long missionId) {
        Long count = queryFactory
                .select(userMission.user.id.countDistinct())
                .from(userMission)
                .where(userMission.mission.id.eq(missionId)
                        .and(userMission.mission.id.isNotNull()))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public java.util.Map<Long, Long> countDistinctUsersByMissionIds(List<Long> missionIds) {
        if (missionIds == null || missionIds.isEmpty()) {
            return new java.util.HashMap<>();
        }

        // QueryDSL의 Tuple을 사용하여 미션별 참여자 수 조회
        List<com.querydsl.core.Tuple> results = queryFactory
                .select(userMission.mission.id, userMission.user.id.countDistinct())
                .from(userMission)
                .where(userMission.mission.id.in(missionIds)
                        .and(userMission.mission.id.isNotNull()))
                .groupBy(userMission.mission.id)
                .fetch();

        java.util.Map<Long, Long> participantCountMap = new java.util.HashMap<>();
        for (com.querydsl.core.Tuple tuple : results) {
            Long missionId = tuple.get(userMission.mission.id);
            Long count = tuple.get(userMission.user.id.countDistinct());
            if (missionId != null && count != null) {
                participantCountMap.put(missionId, count);
            }
        }

        // 조회되지 않은 미션은 0으로 설정
        for (Long missionId : missionIds) {
            participantCountMap.putIfAbsent(missionId, 0L);
        }

        return participantCountMap;
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
