package com.app.replant.domain.review.repository;

import com.app.replant.domain.review.entity.MissionReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionReviewRepository extends JpaRepository<MissionReview, Long> {

    @Query("SELECT mr FROM MissionReview mr WHERE mr.mission.id = :missionId")
    Page<MissionReview> findByMissionId(@Param("missionId") Long missionId, Pageable pageable);

    boolean existsByMissionIdAndUserId(Long missionId, Long userId);

    @Query("SELECT COUNT(mr) FROM MissionReview mr WHERE mr.mission.id = :missionId")
    long countByMissionId(@Param("missionId") Long missionId);
}
