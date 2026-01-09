package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.missionset.entity.MissionSetMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissionSetMissionRepository extends JpaRepository<MissionSetMission, Long> {

    // 미션세트에 포함된 미션 목록
    List<MissionSetMission> findByMissionSetOrderByDisplayOrderAsc(MissionSet missionSet);

    // 특정 미션세트에 특정 미션이 있는지 확인
    boolean existsByMissionSetAndMission(MissionSet missionSet, Mission mission);

    // 특정 미션세트에서 특정 미션 찾기
    Optional<MissionSetMission> findByMissionSetAndMission(MissionSet missionSet, Mission mission);

    // 미션세트의 최대 displayOrder 조회
    @Query("SELECT COALESCE(MAX(msm.displayOrder), 0) FROM MissionSetMission msm WHERE msm.missionSet = :missionSet")
    Integer findMaxDisplayOrderByMissionSet(@Param("missionSet") MissionSet missionSet);

    // 미션세트의 미션 수
    long countByMissionSet(MissionSet missionSet);
}
