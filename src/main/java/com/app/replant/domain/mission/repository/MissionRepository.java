package com.app.replant.domain.mission.repository;

import com.app.replant.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Mission Repository
 * QueryDSL을 사용한 복잡한 쿼리는 MissionRepositoryCustom을 통해 구현
 */
public interface MissionRepository extends JpaRepository<Mission, Long>, MissionRepositoryCustom {
}
