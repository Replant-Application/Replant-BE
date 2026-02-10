package com.app.replant.domain.mission.repository;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

import static com.app.replant.domain.mission.entity.QMission.mission;

/**
 * MissionRepository Custom Implementation
 * QueryDSL을 사용한 복잡한 쿼리 구현
 */
@RequiredArgsConstructor
public class MissionRepositoryCustomImpl implements MissionRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    
    @PersistenceContext
    private EntityManager entityManager;

    // ============================================
    // 공통 조건
    // ============================================

    private BooleanExpression isActive() {
        return mission.isActive.isTrue();
    }

    private BooleanExpression isOfficial() {
        return mission.missionType.eq(MissionType.OFFICIAL);
    }

    private BooleanExpression isCustom() {
        return mission.missionType.eq(MissionType.CUSTOM);
    }

    // ============================================
    // 공통 쿼리
    // ============================================

    @Override
    public Page<Mission> findMissions(
            MissionCategory category,
            VerificationType verificationType,
            Pageable pageable) {
        // JPQL을 사용하여 ageRanges를 함께 조회 (N+1 문제 방지)
        StringBuilder jpql = new StringBuilder(
            "SELECT DISTINCT m FROM Mission m " +
            "LEFT JOIN FETCH m.ageRanges " +
            "WHERE m.isActive = true AND m.missionType = :missionType"
        );
        
        if (category != null) {
            jpql.append(" AND m.category = :category");
        }
        if (verificationType != null) {
            jpql.append(" AND m.verificationType = :verificationType");
        }
        jpql.append(" ORDER BY m.id DESC");
        
        jakarta.persistence.Query query = entityManager.createQuery(jpql.toString(), Mission.class);
        query.setParameter("missionType", MissionType.OFFICIAL);
        if (category != null) {
            query.setParameter("category", category);
        }
        if (verificationType != null) {
            query.setParameter("verificationType", verificationType);
        }
        
        // Count 쿼리
        StringBuilder countJpql = new StringBuilder(
            "SELECT COUNT(DISTINCT m.id) FROM Mission m " +
            "WHERE m.isActive = true AND m.missionType = :missionType"
        );
        if (category != null) {
            countJpql.append(" AND m.category = :category");
        }
        if (verificationType != null) {
            countJpql.append(" AND m.verificationType = :verificationType");
        }
        
        jakarta.persistence.Query countQuery = entityManager.createQuery(countJpql.toString(), Long.class);
        countQuery.setParameter("missionType", MissionType.OFFICIAL);
        if (category != null) {
            countQuery.setParameter("category", category);
        }
        if (verificationType != null) {
            countQuery.setParameter("verificationType", verificationType);
        }
        
        long total = ((Number) countQuery.getSingleResult()).longValue();
        
        // 페이징 적용
        if (!pageable.isUnpaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        
        @SuppressWarnings("unchecked")
        List<Mission> content = query.getResultList();
        
        return new PageImpl<>(content, pageable, total);
    }

    // ============================================
    // 공식 미션 쿼리
    // ============================================

    @Override
    public Page<Mission> findOfficialMissions(
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(isActive());
        builder.and(isOfficial());

        if (category != null) {
            builder.and(mission.category.eq(category));
        }
        if (verificationType != null) {
            builder.and(mission.verificationType.eq(verificationType));
        }
        if (worryType != null) {
            builder.and(mission.worryType.eq(worryType));
        }
        if (ageRange != null) {
            // ElementCollection의 경우 contains 사용
            builder.and(mission.ageRanges.contains(ageRange));
        }
        if (genderType != null) {
            builder.and(mission.genderType.eq(genderType).or(mission.genderType.eq(GenderType.ALL)));
        }
        if (regionType != null) {
            builder.and(mission.regionType.eq(regionType).or(mission.regionType.eq(RegionType.ALL)));
        }
        if (difficultyLevel != null) {
            builder.and(mission.difficultyLevel.eq(difficultyLevel));
        }

        JPAQuery<Mission> query = queryFactory
                .selectFrom(mission)
                .where(builder)
                .distinct()
                .orderBy(mission.id.desc());

        return getPage(query, pageable);
    }

    @Override
    public Page<Mission> searchOfficialMissions(
            String keyword,
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(isActive());
        builder.and(isOfficial());

        // 키워드 검색 (제목 또는 설명)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = "%" + keyword.trim() + "%";
            builder.and(mission.title.likeIgnoreCase(searchKeyword)
                    .or(mission.description.likeIgnoreCase(searchKeyword)));
        }

        if (category != null) {
            builder.and(mission.category.eq(category));
        }
        if (verificationType != null) {
            builder.and(mission.verificationType.eq(verificationType));
        }
        if (worryType != null) {
            builder.and(mission.worryType.eq(worryType));
        }
        if (ageRange != null) {
            builder.and(mission.ageRanges.contains(ageRange));
        }
        if (genderType != null) {
            builder.and(mission.genderType.eq(genderType).or(mission.genderType.eq(GenderType.ALL)));
        }
        if (regionType != null) {
            builder.and(mission.regionType.eq(regionType).or(mission.regionType.eq(RegionType.ALL)));
        }
        if (difficultyLevel != null) {
            builder.and(mission.difficultyLevel.eq(difficultyLevel));
        }

        JPAQuery<Mission> query = queryFactory
                .selectFrom(mission)
                .where(builder)
                .distinct()
                .orderBy(mission.id.desc());

        return getPage(query, pageable);
    }

    @Override
    public List<Mission> findActiveOfficialByCategory(MissionCategory category) {
        return queryFactory
                .selectFrom(mission)
                .where(isActive()
                        .and(isOfficial())
                        .and(mission.category.eq(category)))
                .fetch();
    }

    @Override
    public long countActiveOfficialByCategory(MissionCategory category) {
        Long count = queryFactory
                .select(mission.count())
                .from(mission)
                .where(isActive()
                        .and(isOfficial())
                        .and(mission.category.eq(category)))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public Optional<Mission> findOfficialMissionById(Long missionId) {
        Mission result = queryFactory
                .selectFrom(mission)
                .where(mission.id.eq(missionId)
                        .and(isOfficial()))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    // ============================================
    // 커스텀 미션 쿼리
    // ============================================

    @Override
    public Page<Mission> findCustomMissionsByCreator(Long creatorId, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(isCustom());
        builder.and(mission.creator.id.eq(creatorId));
        builder.and(isActive());

        JPAQuery<Mission> query = queryFactory
                .selectFrom(mission)
                .where(builder)
                .orderBy(mission.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public Page<Mission> findPublicCustomMissions(
            WorryType worryType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(isCustom());
        builder.and(mission.isPublic.isTrue());
        builder.and(isActive());

        if (worryType != null) {
            builder.and(mission.worryType.eq(worryType));
        }
        if (difficultyLevel != null) {
            builder.and(mission.difficultyLevel.eq(difficultyLevel));
        }

        JPAQuery<Mission> query = queryFactory
                .selectFrom(mission)
                .where(builder)
                .orderBy(mission.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public Page<Mission> findCustomMissions(Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(isCustom());
        builder.and(mission.isPublic.isTrue());
        builder.and(isActive());

        JPAQuery<Mission> query = queryFactory
                .selectFrom(mission)
                .where(builder)
                .orderBy(mission.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public Page<Mission> searchCustomMissions(
            String keyword,
            Boolean titleOnly,
            WorryType worryType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(isCustom());
        builder.and(mission.isPublic.isTrue());
        builder.and(isActive());

        // 키워드 검색
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = "%" + keyword.trim() + "%";
            if (Boolean.TRUE.equals(titleOnly)) {
                // 제목만 검색
                builder.and(mission.title.likeIgnoreCase(searchKeyword));
            } else {
                // 제목 또는 설명 검색
                builder.and(mission.title.likeIgnoreCase(searchKeyword)
                        .or(mission.description.likeIgnoreCase(searchKeyword)));
            }
        }

        if (worryType != null) {
            builder.and(mission.worryType.eq(worryType));
        }
        if (difficultyLevel != null) {
            builder.and(mission.difficultyLevel.eq(difficultyLevel));
        }

        JPAQuery<Mission> query = queryFactory
                .selectFrom(mission)
                .where(builder)
                .orderBy(mission.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public Optional<Mission> findCustomMissionById(Long missionId) {
        Mission result = queryFactory
                .selectFrom(mission)
                .where(mission.id.eq(missionId)
                        .and(isCustom()))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public long countCustomMissionsByCreator(Long creatorId) {
        Long count = queryFactory
                .select(mission.count())
                .from(mission)
                .where(isCustom()
                        .and(mission.creator.id.eq(creatorId))
                        .and(isActive()))
                .fetchOne();

        return count != null ? count : 0L;
    }

    // ============================================
    // Deprecated 쿼리
    // ============================================

    @Override
    @Deprecated
    public Page<Mission> findFilteredMissions(
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        // findOfficialMissions와 동일한 구현
        return findOfficialMissions(
                category, verificationType, worryType, ageRange,
                genderType, regionType, difficultyLevel, pageable);
    }

    @Override
    @Deprecated
    public List<Mission> findActiveByCategory(MissionCategory category) {
        return findActiveOfficialByCategory(category);
    }

    @Override
    @Deprecated
    public long countActiveByCategory(MissionCategory category) {
        return countActiveOfficialByCategory(category);
    }

    // ============================================
    // 투두리스트용 쿼리
    // ============================================

    @Override
    public List<Mission> findRandomOfficialNonChallengeMissions(int count) {
        // Native query는 EntityManager 사용
        String sql = "SELECT * FROM mission m WHERE m.mission_type = 'OFFICIAL' " +
                    "AND m.is_active = true " +
                    "ORDER BY RAND() LIMIT :count";
        Query query = entityManager.createNativeQuery(sql, Mission.class);
        query.setParameter("count", count);
        @SuppressWarnings("unchecked")
        List<Mission> results = query.getResultList();
        return results;
    }

    @Override
    public List<Mission> findRandomOfficialNonChallengeMissionsByCategory(int count, MissionCategory category) {
        if (category == null) {
            return findRandomOfficialNonChallengeMissions(count);
        }
        String sql = "SELECT * FROM mission m WHERE m.mission_type = 'OFFICIAL' " +
                    "AND m.is_active = true AND m.category = :category " +
                    "ORDER BY RAND() LIMIT :count";
        Query query = entityManager.createNativeQuery(sql, Mission.class);
        query.setParameter("category", category.name());
        query.setParameter("count", count);
        @SuppressWarnings("unchecked")
        List<Mission> results = query.getResultList();
        return results;
    }

    @Override
    public Optional<Mission> findRandomOfficialNonChallengeMissionExcluding(List<Long> excludeMissionIds) {
        // Native query는 EntityManager 사용
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM mission m WHERE m.mission_type = 'OFFICIAL' " +
            "AND m.is_active = true "
        );
        
        // 제외할 미션 ID가 있으면 조건 추가
        if (excludeMissionIds != null && !excludeMissionIds.isEmpty()) {
            sql.append("AND m.id NOT IN (");
            for (int i = 0; i < excludeMissionIds.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append(":excludeId").append(i);
            }
            sql.append(") ");
        }
        
        sql.append("ORDER BY RAND() LIMIT 1");
        
        Query query = entityManager.createNativeQuery(sql.toString(), Mission.class);
        
        // 제외할 미션 ID 파라미터 설정
        if (excludeMissionIds != null && !excludeMissionIds.isEmpty()) {
            for (int i = 0; i < excludeMissionIds.size(); i++) {
                query.setParameter("excludeId" + i, excludeMissionIds.get(i));
            }
        }
        
        @SuppressWarnings("unchecked")
        List<Mission> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Mission> findRandomOfficialNonChallengeMissionExcludingByCategory(List<Long> excludeMissionIds, MissionCategory category) {
        if (category == null) {
            return findRandomOfficialNonChallengeMissionExcluding(excludeMissionIds);
        }
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM mission m WHERE m.mission_type = 'OFFICIAL' " +
            "AND m.is_active = true AND m.category = :category "
        );
        if (excludeMissionIds != null && !excludeMissionIds.isEmpty()) {
            sql.append("AND m.id NOT IN (");
            for (int i = 0; i < excludeMissionIds.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append(":excludeId").append(i);
            }
            sql.append(") ");
        }
        sql.append("ORDER BY RAND() LIMIT 1");
        Query query = entityManager.createNativeQuery(sql.toString(), Mission.class);
        query.setParameter("category", category.name());
        if (excludeMissionIds != null && !excludeMissionIds.isEmpty()) {
            for (int i = 0; i < excludeMissionIds.size(); i++) {
                query.setParameter("excludeId" + i, excludeMissionIds.get(i));
            }
        }
        @SuppressWarnings("unchecked")
        List<Mission> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Mission> findRandomOfficialNonChallengeMissionsByCategories(int count, List<MissionCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return findRandomOfficialNonChallengeMissions(count);
        }
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM mission m WHERE m.mission_type = 'OFFICIAL' AND m.is_active = true AND m.category IN ("
        );
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(":cat").append(i);
        }
        sql.append(") ORDER BY RAND() LIMIT :count");
        Query query = entityManager.createNativeQuery(sql.toString(), Mission.class);
        for (int i = 0; i < categories.size(); i++) {
            query.setParameter("cat" + i, categories.get(i).name());
        }
        query.setParameter("count", count);
        @SuppressWarnings("unchecked")
        List<Mission> results = query.getResultList();
        return results;
    }

    @Override
    public Optional<Mission> findRandomOfficialNonChallengeMissionExcludingByCategories(List<Long> excludeMissionIds, List<MissionCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return findRandomOfficialNonChallengeMissionExcluding(excludeMissionIds);
        }
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM mission m WHERE m.mission_type = 'OFFICIAL' AND m.is_active = true AND m.category IN ("
        );
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(":cat").append(i);
        }
        sql.append(") ");
        if (excludeMissionIds != null && !excludeMissionIds.isEmpty()) {
            sql.append("AND m.id NOT IN (");
            for (int i = 0; i < excludeMissionIds.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append(":excludeId").append(i);
            }
            sql.append(") ");
        }
        sql.append("ORDER BY RAND() LIMIT 1");
        Query query = entityManager.createNativeQuery(sql.toString(), Mission.class);
        for (int i = 0; i < categories.size(); i++) {
            query.setParameter("cat" + i, categories.get(i).name());
        }
        if (excludeMissionIds != null && !excludeMissionIds.isEmpty()) {
            for (int i = 0; i < excludeMissionIds.size(); i++) {
                query.setParameter("excludeId" + i, excludeMissionIds.get(i));
            }
        }
        @SuppressWarnings("unchecked")
        List<Mission> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Mission> findNonChallengeCustomMissionsByCreator(Long creatorId) {
        return queryFactory
                .selectFrom(mission)
                .where(isCustom()
                        .and(mission.creator.id.eq(creatorId))
                        .and(isActive()))
                .orderBy(mission.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Mission> findAllPublicNonChallengeCustomMissions() {
        return queryFactory
                .selectFrom(mission)
                .where(isCustom()
                        .and(isActive())
                        .and(mission.isPublic.isTrue()))
                .orderBy(mission.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Mission> findByIdIn(List<Long> missionIds) {
        if (missionIds == null || missionIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .selectFrom(mission)
                .where(mission.id.in(missionIds))
                .fetch();
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    /**
     * QueryDSL 쿼리를 Page로 변환
     */
    private Page<Mission> getPage(JPAQuery<Mission> query, Pageable pageable) {
        // Pageable.unpaged()인 경우 전체 조회
        if (pageable.isUnpaged()) {
            List<Mission> content = query.fetch();
            return new PageImpl<>(content, pageable, content.size());
        }
        
        // 페이징 적용
        JPAQuery<Mission> pagedQuery = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // Count 쿼리 - JOIN 없이 where 조건만 복사
        com.querydsl.core.types.Predicate whereCondition = query.getMetadata().getWhere();
        JPAQuery<Long> countQuery = queryFactory
                .select(mission.count())
                .from(mission)
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
