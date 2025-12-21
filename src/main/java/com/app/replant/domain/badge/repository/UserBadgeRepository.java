package com.app.replant.domain.badge.repository;

import com.app.replant.domain.badge.entity.UserBadge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    @Query("SELECT ub FROM UserBadge ub WHERE ub.user.id = :userId AND ub.expiresAt > :now")
    List<UserBadge> findValidBadgesByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT ub FROM UserBadge ub WHERE ub.user.id = :userId")
    Page<UserBadge> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(ub) > 0 THEN true ELSE false END " +
           "FROM UserBadge ub WHERE ub.user.id = :userId " +
           "AND ub.userMission.mission.id = :missionId " +
           "AND ub.expiresAt > :now")
    boolean hasValidBadgeForMission(@Param("userId") Long userId, @Param("missionId") Long missionId, @Param("now") LocalDateTime now);
}
