package com.app.replant.domain.mission.repository;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @Query("SELECT m FROM Mission m WHERE m.isActive = true " +
           "AND (:type IS NULL OR m.type = :type) " +
           "AND (:verificationType IS NULL OR m.verificationType = :verificationType)")
    Page<Mission> findMissions(
        @Param("type") MissionType type,
        @Param("verificationType") VerificationType verificationType,
        Pageable pageable
    );

    /**
     * 사용자 맞춤 필터링 미션 목록 조회
     */
    @Query("SELECT DISTINCT m FROM Mission m LEFT JOIN m.ageRanges ar WHERE m.isActive = true " +
           "AND (:type IS NULL OR m.type = :type) " +
           "AND (:verificationType IS NULL OR m.verificationType = :verificationType) " +
           "AND (:worryType IS NULL OR m.worryType = :worryType) " +
           "AND (:ageRange IS NULL OR ar = :ageRange) " +
           "AND (:genderType IS NULL OR m.genderType = :genderType OR m.genderType = 'ALL') " +
           "AND (:regionType IS NULL OR m.regionType = :regionType OR m.regionType = 'ALL') " +
           "AND (:difficultyLevel IS NULL OR m.difficultyLevel = :difficultyLevel)")
    Page<Mission> findFilteredMissions(
        @Param("type") MissionType type,
        @Param("verificationType") VerificationType verificationType,
        @Param("worryType") WorryType worryType,
        @Param("ageRange") AgeRange ageRange,
        @Param("genderType") GenderType genderType,
        @Param("regionType") RegionType regionType,
        @Param("difficultyLevel") DifficultyLevel difficultyLevel,
        Pageable pageable
    );

    /**
     * 타입별 활성화된 미션 목록 조회
     */
    @Query("SELECT m FROM Mission m WHERE m.isActive = true AND m.type = :type")
    List<Mission> findActiveByType(@Param("type") MissionType type);

    /**
     * 타입별 활성화된 미션 개수
     */
    @Query("SELECT COUNT(m) FROM Mission m WHERE m.isActive = true AND m.type = :type")
    long countActiveByType(@Param("type") MissionType type);
}
