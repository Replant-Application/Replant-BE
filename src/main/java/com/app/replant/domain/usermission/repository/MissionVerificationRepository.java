package com.app.replant.domain.usermission.repository;

import com.app.replant.domain.usermission.entity.MissionVerification;
import com.app.replant.domain.usermission.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionVerificationRepository extends JpaRepository<MissionVerification, Long> {
    Optional<MissionVerification> findByUserMission(UserMission userMission);
}
