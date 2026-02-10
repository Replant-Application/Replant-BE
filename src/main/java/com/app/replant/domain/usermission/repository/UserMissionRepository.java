package com.app.replant.domain.usermission.repository;

import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserMission Repository
 * QueryDSL을 사용한 복잡한 쿼리는 UserMissionRepositoryCustom을 통해 구현
 */
public interface UserMissionRepository extends JpaRepository<UserMission, Long>, UserMissionRepositoryCustom {

    /**
     * 투두리스트 삭제 시 해당 날짜·미션에 해당하는 나의 미션(ASSIGNED/PENDING) 조회.
     * 투두리스트 생성 시 같은 날 assignedAt으로 UserMission이 생성되므로, 같은 날짜+미션ID로 삭제 대상 식별.
     */
    List<UserMission> findByUser_IdAndMission_IdInAndStatusInAndAssignedAtBetween(
            Long userId,
            List<Long> missionIds,
            List<UserMissionStatus> statuses,
            LocalDateTime assignedAtStart,
            LocalDateTime assignedAtEnd);
    
    // TODO: isSpontaneous 필드가 삭제되어 임시로 주석 처리
    // 필요시 mission IS NULL 조건으로 QueryDSL로 구현 필요
    // Optional<UserMission> findTopByUserIdAndIsSpontaneousAndStatusOrderByAssignedAtDesc(
    //         Long userId, Boolean isSpontaneous, UserMissionStatus status);
}
