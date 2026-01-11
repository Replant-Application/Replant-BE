package com.app.replant.domain.custommission.entity;

import com.app.replant.domain.mission.enums.DifficultyLevel;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.enums.WorryType;
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

    // 고민 종류 (시스템 미션과 동일)
    @Enumerated(EnumType.STRING)
    @Column(name = "worry_type", length = 30)
    private WorryType worryType;

    // 미션 타입 (카테고리): DAILY_LIFE(일상), GROWTH(성장), EXERCISE(운동), STUDY(학습), HEALTH(건강), RELATIONSHIP(관계)
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", length = 20)
    private MissionType missionType;

    // 난이도: EASY(쉬움), MEDIUM(보통), HARD(어려움)
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private DifficultyLevel difficultyLevel;

    // 챌린지 미션 여부 (true: 챌린지 미션, false: 일반 미션)
    @Column(name = "is_challenge", nullable = false)
    private Boolean isChallenge;

    // 챌린지 기간 (일수) - 챌린지 미션일 때만 사용 (예: 7일 챌린지면 7)
    @Column(name = "challenge_days")
    private Integer challengeDays;

    // 완료 기한 (일수) - 일반 미션일 때만 사용 (미션 할당 후 N일 이내 완료)
    @Column(name = "deadline_days")
    private Integer deadlineDays;

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

    // 시간 미션용 시작/종료 시간 (HH:mm 형식)
    @Column(name = "start_time", length = 5)
    private String startTime;

    @Column(name = "end_time", length = 5)
    private String endTime;

    @Column(name = "exp_reward", nullable = false)
    private Integer expReward;

    @Column(name = "badge_duration_days", nullable = false)
    private Integer badgeDurationDays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    // 공식 미션으로 승격 여부 (별점/평가 기반으로 나중에 승격 시스템 적용)
    @Column(name = "is_promoted", nullable = false)
    private Boolean isPromoted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private CustomMission(User creator, String title, String description, WorryType worryType,
                          MissionType missionType, DifficultyLevel difficultyLevel,
                          Boolean isChallenge, Integer challengeDays, Integer deadlineDays, Integer durationDays,
                          Boolean isPublic, VerificationType verificationType,
                          BigDecimal gpsLatitude, BigDecimal gpsLongitude, Integer gpsRadiusMeters,
                          Integer requiredMinutes, String startTime, String endTime,
                          Integer expReward, Integer badgeDurationDays,
                          Boolean isActive, Boolean isPromoted) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.worryType = worryType;
        this.missionType = missionType;
        this.difficultyLevel = difficultyLevel != null ? difficultyLevel : DifficultyLevel.MEDIUM;
        this.isChallenge = isChallenge != null ? isChallenge : false;  // 기본값: 일반 미션
        // 챌린지 미션이면 challengeDays 설정, 일반 미션이면 deadlineDays 설정
        this.challengeDays = this.isChallenge ? (challengeDays != null ? challengeDays : 7) : null;
        this.deadlineDays = this.isChallenge ? null : (deadlineDays != null ? deadlineDays : 3);
        this.durationDays = durationDays;
        this.isPublic = isPublic != null ? isPublic : false;
        this.verificationType = verificationType;
        this.gpsLatitude = gpsLatitude;
        this.gpsLongitude = gpsLongitude;
        this.gpsRadiusMeters = gpsRadiusMeters != null ? gpsRadiusMeters : 100;
        this.requiredMinutes = requiredMinutes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.expReward = expReward != null ? expReward : calculateDefaultExpReward(difficultyLevel);
        this.badgeDurationDays = badgeDurationDays != null ? badgeDurationDays : 3;
        this.isActive = isActive != null ? isActive : true;
        this.isPromoted = isPromoted != null ? isPromoted : false;
        this.createdAt = LocalDateTime.now();
    }

    // 난이도에 따른 기본 경험치 계산
    private static Integer calculateDefaultExpReward(DifficultyLevel level) {
        if (level == null) return 20;
        return switch (level) {
            case EASY, LEVEL1 -> 10;
            case MEDIUM, LEVEL2 -> 20;
            case HARD, LEVEL3 -> 30;
        };
    }

    public void update(String title, String description, WorryType worryType, MissionType missionType,
                       DifficultyLevel difficultyLevel, Boolean isChallenge, Integer challengeDays,
                       Integer deadlineDays, Integer expReward, Boolean isPublic) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (worryType != null) {
            this.worryType = worryType;
        }
        if (missionType != null) {
            this.missionType = missionType;
        }
        if (difficultyLevel != null) {
            this.difficultyLevel = difficultyLevel;
        }
        if (isChallenge != null) {
            this.isChallenge = isChallenge;
            // 챌린지 여부에 따라 기간 필드 조정
            if (isChallenge) {
                this.deadlineDays = null;
            } else {
                this.challengeDays = null;
            }
        }
        if (challengeDays != null && Boolean.TRUE.equals(this.isChallenge)) {
            this.challengeDays = challengeDays;
        }
        if (deadlineDays != null && Boolean.FALSE.equals(this.isChallenge)) {
            this.deadlineDays = deadlineDays;
        }
        if (expReward != null) {
            this.expReward = expReward;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

    public boolean isCreator(Long userId) {
        return this.creator.getId().equals(userId);
    }
}
