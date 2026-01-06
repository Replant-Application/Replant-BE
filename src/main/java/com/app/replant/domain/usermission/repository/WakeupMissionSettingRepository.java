package com.app.replant.domain.usermission.repository;

import com.app.replant.domain.usermission.entity.WakeupMissionSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WakeupMissionSettingRepository extends JpaRepository<WakeupMissionSetting, Long> {

    @Query("SELECT w FROM WakeupMissionSetting w WHERE w.user.id = :userId AND w.weekNumber = :weekNumber AND w.year = :year AND w.isActive = true")
    Optional<WakeupMissionSetting> findActiveByUserIdAndWeek(@Param("userId") Long userId,
                                                              @Param("weekNumber") Integer weekNumber,
                                                              @Param("year") Integer year);

    @Query("SELECT w FROM WakeupMissionSetting w WHERE w.user.id = :userId AND w.isActive = true ORDER BY w.createdAt DESC")
    Optional<WakeupMissionSetting> findLatestActiveByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndWeekNumberAndYearAndIsActiveTrue(Long userId, Integer weekNumber, Integer year);
}
