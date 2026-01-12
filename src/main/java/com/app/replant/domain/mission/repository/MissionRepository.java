package com.app.replant.domain.mission.repository;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

       // ============================================
       // 공통 쿼리
       // ============================================

       @Query("SELECT m FROM Mission m WHERE m.isActive = true " +
                     "AND m.missionType = 'OFFICIAL' " +
                     "AND (:category IS NULL OR m.category = :category) " +
                     "AND (:verificationType IS NULL OR m.verificationType = :verificationType)")
       Page<Mission> findMissions(
                     @Param("category") MissionCategory category,
                     @Param("verificationType") VerificationType verificationType,
                     Pageable pageable);

       // ============================================
       // 공식 미션 쿼리
       // ============================================

       /**
        * 공식 미션 목록 조회 (사용자 맞춤 필터링)
        */
       @Query("SELECT DISTINCT m FROM Mission m LEFT JOIN m.ageRanges ar " +
                     "WHERE m.isActive = true AND m.missionType = 'OFFICIAL' " +
                     "AND (:category IS NULL OR m.category = :category) " +
                     "AND (:verificationType IS NULL OR m.verificationType = :verificationType) " +
                     "AND (:worryType IS NULL OR m.worryType = :worryType) " +
                     "AND (:ageRange IS NULL OR ar = :ageRange) " +
                     "AND (:genderType IS NULL OR m.genderType = :genderType OR m.genderType = 'ALL') " +
                     "AND (:regionType IS NULL OR m.regionType = :regionType OR m.regionType = 'ALL') " +
                     "AND (:difficultyLevel IS NULL OR m.difficultyLevel = :difficultyLevel)")
       Page<Mission> findOfficialMissions(
                     @Param("category") MissionCategory category,
                     @Param("verificationType") VerificationType verificationType,
                     @Param("worryType") WorryType worryType,
                     @Param("ageRange") AgeRange ageRange,
                     @Param("genderType") GenderType genderType,
                     @Param("regionType") RegionType regionType,
                     @Param("difficultyLevel") DifficultyLevel difficultyLevel,
                     Pageable pageable);

       /**
        * 카테고리별 활성화된 공식 미션 목록 조회
        */
       @Query("SELECT m FROM Mission m WHERE m.isActive = true AND m.missionType = 'OFFICIAL' AND m.category = :category")
       List<Mission> findActiveOfficialByCategory(@Param("category") MissionCategory category);

       /**
        * 카테고리별 활성화된 공식 미션 개수
        */
       @Query("SELECT COUNT(m) FROM Mission m WHERE m.isActive = true AND m.missionType = 'OFFICIAL' AND m.category = :category")
       long countActiveOfficialByCategory(@Param("category") MissionCategory category);

       /**
        * 공식 미션 ID로 조회
        */
       @Query("SELECT m FROM Mission m WHERE m.id = :missionId AND m.missionType = 'OFFICIAL'")
       Optional<Mission> findOfficialMissionById(@Param("missionId") Long missionId);

       // ============================================
       // 커스텀 미션 쿼리
       // ============================================

       /**
        * 커스텀 미션 목록 조회 (특정 사용자가 생성한)
        */
       @Query("SELECT m FROM Mission m WHERE m.missionType = 'CUSTOM' AND m.creator.id = :creatorId " +
                     "AND m.isActive = true ORDER BY m.createdAt DESC")
       Page<Mission> findCustomMissionsByCreator(@Param("creatorId") Long creatorId, Pageable pageable);

       /**
        * 공개 커스텀 미션 목록 조회
        */
       @Query("SELECT m FROM Mission m WHERE m.missionType = 'CUSTOM' AND m.isPublic = true " +
                     "AND m.isActive = true " +
                     "AND (:worryType IS NULL OR m.worryType = :worryType) " +
                     "AND (:difficultyLevel IS NULL OR m.difficultyLevel = :difficultyLevel) " +
                     "ORDER BY m.createdAt DESC")
       Page<Mission> findPublicCustomMissions(
                     @Param("worryType") WorryType worryType,
                     @Param("difficultyLevel") DifficultyLevel difficultyLevel,
                     Pageable pageable);

       /**
        * 공개 커스텀 미션 목록 조회 (인증 타입 필터)
        */
       @Query("SELECT m FROM Mission m WHERE m.missionType = 'CUSTOM' AND m.isPublic = true " +
                     "AND m.isActive = true " +
                     "AND (:verificationType IS NULL OR m.verificationType = :verificationType) " +
                     "ORDER BY m.createdAt DESC")
       Page<Mission> findCustomMissions(
                     @Param("verificationType") VerificationType verificationType,
                     Pageable pageable);

       /**
        * 커스텀 미션 ID로 조회
        */
       @Query("SELECT m FROM Mission m WHERE m.id = :missionId AND m.missionType = 'CUSTOM'")
       Optional<Mission> findCustomMissionById(@Param("missionId") Long missionId);

       /**
        * 사용자의 커스텀 미션 개수
        */
       @Query("SELECT COUNT(m) FROM Mission m WHERE m.missionType = 'CUSTOM' AND m.creator.id = :creatorId AND m.isActive = true")
       long countCustomMissionsByCreator(@Param("creatorId") Long creatorId);

       // ============================================
       // 기존 호환성을 위한 쿼리 (Deprecated - 추후 제거 예정)
       // ============================================

       /**
        * @deprecated Use findOfficialMissions instead
        */
       @Deprecated
       @Query("SELECT DISTINCT m FROM Mission m LEFT JOIN m.ageRanges ar WHERE m.isActive = true " +
                     "AND m.missionType = 'OFFICIAL' " +
                     "AND (:category IS NULL OR m.category = :category) " +
                     "AND (:verificationType IS NULL OR m.verificationType = :verificationType) " +
                     "AND (:worryType IS NULL OR m.worryType = :worryType) " +
                     "AND (:ageRange IS NULL OR ar = :ageRange) " +
                     "AND (:genderType IS NULL OR m.genderType = :genderType OR m.genderType = 'ALL') " +
                     "AND (:regionType IS NULL OR m.regionType = :regionType OR m.regionType = 'ALL') " +
                     "AND (:difficultyLevel IS NULL OR m.difficultyLevel = :difficultyLevel)")
       Page<Mission> findFilteredMissions(
                     @Param("category") MissionCategory category,
                     @Param("verificationType") VerificationType verificationType,
                     @Param("worryType") WorryType worryType,
                     @Param("ageRange") AgeRange ageRange,
                     @Param("genderType") GenderType genderType,
                     @Param("regionType") RegionType regionType,
                     @Param("difficultyLevel") DifficultyLevel difficultyLevel,
                     Pageable pageable);

       /**
        * @deprecated Use findActiveOfficialByCategory instead
        */
       @Deprecated
       @Query("SELECT m FROM Mission m WHERE m.isActive = true AND m.missionType = 'OFFICIAL' AND m.category = :category")
       List<Mission> findActiveByCategory(@Param("category") MissionCategory category);

       /**
        * @deprecated Use countActiveOfficialByCategory instead
        */
       @Deprecated
       @Query("SELECT COUNT(m) FROM Mission m WHERE m.isActive = true AND m.missionType = 'OFFICIAL' AND m.category = :category")
       long countActiveByCategory(@Param("category") MissionCategory category);

       // ============================================
       // 투두리스트용 쿼리
       // ============================================

       /**
        * 랜덤 비챌린지 공식 미션 조회 (투두리스트용)
        * isChallenge가 false이거나 null인 공식 미션을 랜덤으로 조회
        */
       @Query(value = "SELECT * FROM mission m WHERE m.mission_type = 'OFFICIAL' " +
                     "AND m.is_active = true AND (m.is_challenge = false OR m.is_challenge IS NULL) " +
                     "ORDER BY RAND() LIMIT :count", nativeQuery = true)
       List<Mission> findRandomOfficialNonChallengeMissions(@Param("count") int count);

       /**
        * 비챌린지 커스텀 미션 조회 (투두리스트 커스텀 선택용)
        */
       @Query("SELECT m FROM Mission m WHERE m.missionType = 'CUSTOM' AND m.creator.id = :creatorId " +
                     "AND m.isActive = true AND (m.isChallenge = false OR m.isChallenge IS NULL) " +
                     "ORDER BY m.createdAt DESC")
       List<Mission> findNonChallengeCustomMissionsByCreator(@Param("creatorId") Long creatorId);

       /**
        * 미션 ID 목록으로 미션 조회
        */
       @Query("SELECT m FROM Mission m WHERE m.id IN :missionIds")
       List<Mission> findByIdIn(@Param("missionIds") List<Long> missionIds);
}
