package com.app.replant.domain.mission.repository;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
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
