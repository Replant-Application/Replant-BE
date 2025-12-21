package com.app.replant.domain.custommission.entity;

import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_mission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

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
    private CustomMission(User creator, String title, String description, Integer durationDays, Boolean isPublic,
                          VerificationType verificationType, BigDecimal gpsLatitude, BigDecimal gpsLongitude,
                          Integer gpsRadiusMeters, Integer requiredMinutes, Integer expReward, Integer badgeDurationDays, Boolean isActive) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.durationDays = durationDays;
        this.isPublic = isPublic != null ? isPublic : false;
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

    public void update(String title, String description, Boolean isPublic) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

    public boolean isCreator(Long userId) {
        return this.creator.getId().equals(userId);
    }
}
