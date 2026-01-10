package com.app.replant.domain.verification.repository;

import com.app.replant.domain.verification.entity.VerificationPost;
import com.app.replant.domain.verification.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VerificationPostRepository extends JpaRepository<VerificationPost, Long> {

    @Query("SELECT vp FROM VerificationPost vp WHERE " +
           "(:status IS NULL OR vp.status = :status) " +
           "AND (:missionId IS NULL OR vp.userMission.mission.id = :missionId) " +
           "AND (:customMissionId IS NULL OR vp.userMission.mission.id = :customMissionId AND vp.userMission.missionType = 'CUSTOM')")
    Page<VerificationPost> findWithFilters(
        @Param("status") VerificationStatus status,
        @Param("missionId") Long missionId,
        @Param("customMissionId") Long customMissionId,
        Pageable pageable
    );

    @Query("SELECT vp FROM VerificationPost vp WHERE vp.id = :verificationId AND vp.user.id = :userId")
    Optional<VerificationPost> findByIdAndUserId(@Param("verificationId") Long verificationId, @Param("userId") Long userId);

    boolean existsByUserMissionId(Long userMissionId);
}
