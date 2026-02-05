package com.app.replant.domain.usermission.repository;

import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * UserMission Repository
 * QueryDSL을 사용한 복잡한 쿼리는 UserMissionRepositoryCustom을 통해 구현
 */
public interface UserMissionRepository extends JpaRepository<UserMission, Long>, UserMissionRepositoryCustom {

    /** 투두리스트에서 생성된 UserMission 목록 조회 (삭제 가능 여부 검사용) */
    List<UserMission> findByTodoList_Id(Long todoListId);

    /** 사용자의 가장 최근 ASSIGNED 돌발 미션 (기상 미션 조회 폴백용, KST 날짜 불일치 시) */
    // TODO: isSpontaneous 필드가 삭제되어 임시로 주석 처리
    // 필요시 mission IS NULL 조건으로 QueryDSL로 구현 필요
    // Optional<UserMission> findTopByUserIdAndIsSpontaneousAndStatusOrderByAssignedAtDesc(
    //         Long userId, Boolean isSpontaneous, UserMissionStatus status);

    /** 투두리스트에서 생성된 UserMission 전부 삭제 (투두리스트 Hard Delete 시 나의 미션/캘린더에서도 제거) */
    void deleteByTodoList_Id(Long todoListId);
}
