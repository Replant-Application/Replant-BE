package com.app.replant.domain.custommission.repository;

import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.mission.enums.VerificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomMissionRepository extends JpaRepository<CustomMission, Long> {

    @Query("SELECT cm FROM CustomMission cm WHERE cm.isPublic = true " +
           "AND (:verificationType IS NULL OR cm.verificationType = :verificationType)")
    Page<CustomMission> findPublicMissions(
        @Param("verificationType") VerificationType verificationType,
        Pageable pageable
    );
}
