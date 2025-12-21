package com.app.replant.domain.mission.entity;

import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 20)
    private VerificationType verificationType;

    @Column(name = "gps_latitude", precision = 10, scale = 8)
    private BigDecimal gpsLatitude;

    @Column(name = "gps_longitude", precision = 11, scale = 8)
    private BigDecimal gpsLongitude;

    @Column(name = "gps_radius_meters")
    private Integer gpsRadiusMeters;

    @Column(name = "required_minutes")
    private Integer requiredMinutes;

    @Column(name = "exp_reward", nullable = false)
    private Integer expReward;

    @Column(name = "badge_duration_days", nullable = false)
    private Integer badgeDurationDays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Mission(String title, String description, MissionType type, VerificationType verificationType,
                    BigDecimal gpsLatitude, BigDecimal gpsLongitude, Integer gpsRadiusMeters,
                    Integer requiredMinutes, Integer expReward, Integer badgeDurationDays, Boolean isActive) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.verificationType = verificationType;
        this.gpsLatitude = gpsLatitude;
        this.gpsLongitude = gpsLongitude;
        this.gpsRadiusMeters = gpsRadiusMeters != null ? gpsRadiusMeters : 100;
        this.requiredMinutes = requiredMinutes;
        this.expReward = expReward != null ? expReward : 10;
        this.badgeDurationDays = badgeDurationDays != null ? badgeDurationDays : 3;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = LocalDateTime.now();
    }
}
