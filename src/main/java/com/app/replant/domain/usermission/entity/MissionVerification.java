package com.app.replant.domain.usermission.entity;

import com.app.replant.domain.verification.entity.VerificationPost;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_mission_id", nullable = false, unique = true)
    private UserMission userMission;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_post_id", unique = true)
    private VerificationPost verificationPost;

    @Column(name = "gps_latitude", precision = 10, scale = 8)
    private BigDecimal gpsLatitude;

    @Column(name = "gps_longitude", precision = 11, scale = 8)
    private BigDecimal gpsLongitude;

    @Column(name = "gps_distance_meters")
    private Integer gpsDistanceMeters;

    @Column(name = "time_started_at")
    private LocalDateTime timeStartedAt;

    @Column(name = "time_ended_at")
    private LocalDateTime timeEndedAt;

    @Column(name = "time_actual_minutes")
    private Integer timeActualMinutes;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MissionVerification(UserMission userMission, VerificationPost verificationPost, BigDecimal gpsLatitude,
                                BigDecimal gpsLongitude, Integer gpsDistanceMeters, LocalDateTime timeStartedAt,
                                LocalDateTime timeEndedAt, Integer timeActualMinutes, LocalDateTime verifiedAt) {
        this.userMission = userMission;
        this.verificationPost = verificationPost;
        this.gpsLatitude = gpsLatitude;
        this.gpsLongitude = gpsLongitude;
        this.gpsDistanceMeters = gpsDistanceMeters;
        this.timeStartedAt = timeStartedAt;
        this.timeEndedAt = timeEndedAt;
        this.timeActualMinutes = timeActualMinutes;
        this.verifiedAt = verifiedAt != null ? verifiedAt : LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
}
