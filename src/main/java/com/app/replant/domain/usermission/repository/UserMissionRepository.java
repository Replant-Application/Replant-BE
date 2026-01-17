package com.app.replant.domain.usermission.repository;

import com.app.replant.domain.usermission.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * UserMission Repository
 * QueryDSL을 사용한 복잡한 쿼리는 UserMissionRepositoryCustom을 통해 구현
 */
public interface UserMissionRepository extends JpaRepository<UserMission, Long>, UserMissionRepositoryCustom {
}
