package com.app.replant.domain.mission.repository;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * MissionRepository Custom Interface
 * QueryDSL을 사용한 복잡한 쿼리 메서드 정의
 */
public interface MissionRepositoryCustom {

    // ============================================
    // 공통 쿼리
    // ============================================

    Page<Mission> findMissions(
            MissionCategory category,
            VerificationType verificationType,
            Pageable pageable);

    // ============================================
    // 공식 미션 쿼리
    // ============================================

    /**
     * 공식 미션 목록 조회 (사용자 맞춤 필터링)
     */
    Page<Mission> findOfficialMissions(
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable);

    /**
     * 공식 미션 검색 (제목/설명 검색 + 필터링)
     */
    Page<Mission> searchOfficialMissions(
            String keyword,
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable);

    List<Mission> findActiveOfficialByCategory(MissionCategory category);

    long countActiveOfficialByCategory(MissionCategory category);

    Optional<Mission> findOfficialMissionById(Long missionId);

    // ============================================
    // 커스텀 미션 쿼리
    // ============================================

    Page<Mission> findCustomMissionsByCreator(Long creatorId, Pageable pageable);

    Page<Mission> findPublicCustomMissions(
            WorryType worryType,
            DifficultyLevel difficultyLevel,
            Pageable pageable);

    Page<Mission> findCustomMissions(Pageable pageable);

    /**
     * 커스텀 미션 검색 (제목/설명 검색 + 필터링)
     * @param keyword 검색 키워드 (제목 또는 설명)
     * @param titleOnly true인 경우 제목만 검색, false이거나 null인 경우 제목과 설명 모두 검색
     * @param worryType 고민 타입 필터
     * @param difficultyLevel 난이도 필터
     * @param pageable 페이징 정보
     */
    Page<Mission> searchCustomMissions(
            String keyword,
            Boolean titleOnly,
            WorryType worryType,
            DifficultyLevel difficultyLevel,
            Pageable pageable);

    Optional<Mission> findCustomMissionById(Long missionId);

    long countCustomMissionsByCreator(Long creatorId);

    // ============================================
    // 기존 호환성을 위한 쿼리 (Deprecated)
    // ============================================

    @Deprecated
    Page<Mission> findFilteredMissions(
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable);

    @Deprecated
    List<Mission> findActiveByCategory(MissionCategory category);

    @Deprecated
    long countActiveByCategory(MissionCategory category);

    // ============================================
    // 투두리스트용 쿼리
    // ============================================

    /**
     * 랜덤 공식 미션 조회 (투두리스트용)
     * Native query는 EntityManager를 사용하여 구현
     */
    List<Mission> findRandomOfficialNonChallengeMissions(int count);

    /**
     * 랜덤 공식 미션 조회 (투두리스트용, 특정 카테고리만)
     */
    List<Mission> findRandomOfficialNonChallengeMissionsByCategory(int count, MissionCategory category);

    /**
     * 랜덤 공식 미션 1개 조회 (리롤용, 특정 미션 ID 제외)
     * Native query는 EntityManager를 사용하여 구현
     */
    Optional<Mission> findRandomOfficialNonChallengeMissionExcluding(List<Long> excludeMissionIds);

    /**
     * 랜덤 공식 미션 1개 조회 (리롤용, 특정 미션 ID 제외, 특정 카테고리만)
     */
    Optional<Mission> findRandomOfficialNonChallengeMissionExcludingByCategory(List<Long> excludeMissionIds, MissionCategory category);

    /**
     * 랜덤 공식 미션 조회 (투두리스트용, 여러 카테고리 중 하나)
     */
    List<Mission> findRandomOfficialNonChallengeMissionsByCategories(int count, List<MissionCategory> categories);

    /**
     * 랜덤 공식 미션 1개 조회 (리롤용, 특정 미션 ID 제외, 여러 카테고리 중 하나)
     */
    Optional<Mission> findRandomOfficialNonChallengeMissionExcludingByCategories(List<Long> excludeMissionIds, List<MissionCategory> categories);

    List<Mission> findNonChallengeCustomMissionsByCreator(Long creatorId);

    List<Mission> findAllPublicNonChallengeCustomMissions();

    List<Mission> findByIdIn(List<Long> missionIds);
}
