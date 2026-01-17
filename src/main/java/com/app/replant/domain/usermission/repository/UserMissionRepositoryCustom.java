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
     * 사용자별 미션 목록 조회 - 오늘 할당된 미션만 반환
     * 
     * 투두리스트 개념을 유지하기 위해 오늘 날짜에 할당된 미션만 조회합니다.
     * 전날 할당된 미션은 완료 여부와 관계없이 제외되어 다음날 조회되지 않습니다.
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 오늘 날짜에 할당된 미션 목록 (ASSIGNED, PENDING, COMPLETED 상태 모두 포함)
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

    /**
     * 특정 날짜에 할당된 미션 조회 (캘린더용 - 상태 무관)
     * @param userId 사용자 ID
     * @param date 조회할 날짜 (assignedAt 기준)
     * @return 해당 날짜에 할당된 모든 미션 (ASSIGNED, PENDING, COMPLETED 등 상태 무관)
     */
    List<UserMission> findByUserIdAndAssignedDate(Long userId, java.time.LocalDate date);

    /**
     * 날짜 범위에 할당된 미션 조회 (캘린더용 - 상태 무관)
     * @param userId 사용자 ID
     * @param startDate 시작 날짜 (포함)
     * @param endDate 종료 날짜 (포함)
     * @return 해당 기간에 할당된 모든 미션 (ASSIGNED, PENDING, COMPLETED 등 상태 무관)
     */
    List<UserMission> findByUserIdAndAssignedDateRange(Long userId, java.time.LocalDate startDate, java.time.LocalDate endDate);
}
