package com.app.replant.domain.usermission.repository;

import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserMissionRepository Custom Interface
 * QueryDSL을 사용한 복잡한 쿼리 메서드 정의
 */
public interface UserMissionRepositoryCustom {

    /**
     * 사용자별 미션 목록 조회 (ASSIGNED 또는 PENDING 상태)
     */
    Page<UserMission> findByUserIdWithFilters(Long userId, Pageable pageable);

    /**
     * 사용자 미션 단건 조회
     */
    Optional<UserMission> findByIdAndUserId(Long userMissionId, Long userId);

    /**
     * 사용자별 상태별 미션 개수
     */
    long countByUserIdAndStatus(Long userId, UserMissionStatus status);

    /**
     * 특정 미션을 최근에 완료한 다른 유저들 조회 (추천용)
     */
    List<UserMission> findRecentCompletedByMissionExcludingUser(
            Long missionId,
            Long excludeUserId,
            Pageable pageable);

    /**
     * 특정 커스텀 미션을 최근에 완료한 다른 유저들 조회 (추천용)
     */
    List<UserMission> findRecentCompletedByCustomMissionExcludingUser(
            Long customMissionId,
            Long excludeUserId,
            Pageable pageable);

    /**
     * 유저별 완료된 미션 이력 조회
     */
    Page<UserMission> findMissionHistoryByUserId(Long userId, Pageable pageable);

    /**
     * 특정 유저와 미션으로 UserMission 조회 (ASSIGNED 상태만)
     */
    List<UserMission> findByUserIdAndMissionIdAndStatusAssigned(
            Long userId,
            Long missionId);

    /**
     * 특정 유저와 미션으로 UserMission 조회 (상태 무관)
     */
    List<UserMission> findByUserIdAndMissionId(
            Long userId,
            Long missionId);

    /**
     * 만료된 미션 조회 (기한이 지난 ASSIGNED 상태의 미션)
     */
    List<UserMission> findExpiredMissions(LocalDateTime now);

    /**
     * 특정 유저와 여러 미션 ID로 UserMission 일괄 조회
     */
    List<UserMission> findByUserIdAndMissionIds(
            Long userId,
            List<Long> missionIds);
}
