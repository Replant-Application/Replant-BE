package com.app.replant.domain.badge.repository;

import com.app.replant.domain.badge.entity.UserBadge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    @Query("SELECT ub FROM UserBadge ub " +
           "LEFT JOIN FETCH ub.mission " +
           "LEFT JOIN FETCH ub.userMission um " +
           "LEFT JOIN FETCH um.mission " +
           "WHERE ub.user.id = :userId AND ub.expiresAt > :now AND (um.isSpontaneous = false)")
    List<UserBadge> findValidBadgesByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query(value = "SELECT ub FROM UserBadge ub " +
           "LEFT JOIN FETCH ub.mission " +
           "LEFT JOIN FETCH ub.userMission um " +
           "LEFT JOIN FETCH um.mission " +
           "WHERE ub.user.id = :userId AND (um.isSpontaneous = false)",
           countQuery = "SELECT COUNT(ub) FROM UserBadge ub WHERE ub.user.id = :userId AND (ub.userMission.isSpontaneous = false)")
    Page<UserBadge> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(ub) > 0 THEN true ELSE false END " +
           "FROM UserBadge ub WHERE ub.user.id = :userId " +
           "AND ub.userMission.mission.id = :missionId " +
           "AND ub.expiresAt > :now")
    boolean hasValidBadgeForMission(@Param("userId") Long userId, @Param("missionId") Long missionId, @Param("now") LocalDateTime now);

    @Query("SELECT ub FROM UserBadge ub WHERE ub.user.id = :userId " +
           "AND ub.userMission.mission.id = :missionId " +
           "AND ub.expiresAt > :now ORDER BY ub.issuedAt DESC LIMIT 1")
    Optional<UserBadge> findValidBadgeForMission(@Param("userId") Long userId, @Param("missionId") Long missionId, @Param("now") LocalDateTime now);

    /**
     * UserMission으로 배지 조회 (인증 게시글 삭제 시 배지 삭제용)
     */
    Optional<UserBadge> findByUserMissionId(Long userMissionId);
}
