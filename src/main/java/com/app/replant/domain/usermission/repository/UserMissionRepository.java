package com.app.replant.domain.usermission.repository;

import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

       @Query("SELECT um FROM UserMission um WHERE um.user.id = :userId " +
                     "AND (:status IS NULL OR um.status = :status) " +
                     "AND (:missionType IS NULL OR " +
                     "   (:missionType = 'SYSTEM' AND um.mission IS NOT NULL) OR " +
                     "   (:missionType = 'CUSTOM' AND um.missionType = 'CUSTOM'))")
       Page<UserMission> findByUserIdWithFilters(
                     @Param("userId") Long userId,
                     @Param("status") UserMissionStatus status,
                     @Param("missionType") String missionType,
                     Pageable pageable);

       @Query("SELECT um FROM UserMission um WHERE um.id = :userMissionId AND um.user.id = :userId")
       Optional<UserMission> findByIdAndUserId(@Param("userMissionId") Long userMissionId,
                     @Param("userId") Long userId);

       @Query("SELECT COUNT(um) FROM UserMission um WHERE um.user.id = :userId AND um.status = :status")
       long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") UserMissionStatus status);

       /**
        * 특정 미션을 최근에 완료한 다른 유저들 조회 (추천용)
        */
       @Query("SELECT um FROM UserMission um " +
                     "WHERE um.mission.id = :missionId " +
                     "AND um.status = 'COMPLETED' " +
                     "AND um.user.id != :excludeUserId " +
                     "ORDER BY um.createdAt DESC")
       List<UserMission> findRecentCompletedByMissionExcludingUser(
                     @Param("missionId") Long missionId,
                     @Param("excludeUserId") Long excludeUserId,
                     org.springframework.data.domain.Pageable pageable);

       /**
        * 특정 커스텀 미션을 최근에 완료한 다른 유저들 조회 (추천용)
        */
       @Query("SELECT um FROM UserMission um " +
                     "WHERE um.mission.id = :customMissionId AND um.missionType = 'CUSTOM' " +
                     "AND um.status = 'COMPLETED' " +
                     "AND um.user.id != :excludeUserId " +
                     "ORDER BY um.createdAt DESC")
       List<UserMission> findRecentCompletedByCustomMissionExcludingUser(
                     @Param("customMissionId") Long customMissionId,
                     @Param("excludeUserId") Long excludeUserId,
                     org.springframework.data.domain.Pageable pageable);

       /**
        * 유저별 완료된 미션 이력 조회
        */
       @Query("SELECT um FROM UserMission um WHERE um.user.id = :userId AND um.status = 'COMPLETED' ORDER BY um.createdAt DESC")
       Page<UserMission> findMissionHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

       /**
        * 특정 유저와 미션으로 UserMission 조회 (ASSIGNED 상태만)
        * 중복이 있을 수 있으므로 최신 것만 반환 (ORDER BY id DESC)
        */
       @Query("SELECT um FROM UserMission um WHERE um.user.id = :userId AND um.mission.id = :missionId AND um.status = 'ASSIGNED' ORDER BY um.id DESC")
       List<UserMission> findByUserIdAndMissionIdAndStatusAssigned(
                     @Param("userId") Long userId,
                     @Param("missionId") Long missionId);

       /**
        * 특정 유저와 미션으로 UserMission 조회 (상태 무관)
        * 중복이 있을 수 있으므로 최신 것만 반환 (ORDER BY id DESC)
        * User와 Mission을 함께 조회하여 N+1 문제 방지
        */
       @Query("SELECT DISTINCT um FROM UserMission um " +
                     "LEFT JOIN FETCH um.user " +
                     "LEFT JOIN FETCH um.mission " +
                     "WHERE um.user.id = :userId AND um.mission.id = :missionId " +
                     "ORDER BY um.id DESC")
       List<UserMission> findByUserIdAndMissionId(
                     @Param("userId") Long userId,
                     @Param("missionId") Long missionId);

       /**
        * 만료된 미션 조회 (기한이 지난 ASSIGNED 상태의 미션)
        */
       @Query("SELECT um FROM UserMission um WHERE um.status = 'ASSIGNED' AND um.dueDate < :now")
       List<UserMission> findExpiredMissions(@Param("now") java.time.LocalDateTime now);

       /**
        * 특정 유저와 여러 미션 ID로 UserMission 일괄 조회 (N+1 문제 방지)
        * User와 Mission을 함께 조회하여 지연 로딩 방지
        */
       @Query("SELECT DISTINCT um FROM UserMission um " +
                     "LEFT JOIN FETCH um.user " +
                     "LEFT JOIN FETCH um.mission " +
                     "WHERE um.user.id = :userId AND um.mission.id IN :missionIds " +
                     "ORDER BY um.mission.id, um.id DESC")
       List<UserMission> findByUserIdAndMissionIds(
                     @Param("userId") Long userId,
                     @Param("missionIds") List<Long> missionIds);
}
