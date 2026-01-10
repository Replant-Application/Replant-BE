package com.app.replant.domain.mission.entity;

import com.app.replant.domain.mission.enums.*;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mission", indexes = {
    @Index(name = "idx_mission_source", columnList = "mission_source"),
    @Index(name = "idx_mission_creator", columnList = "creator_id"),
    @Index(name = "idx_mission_is_active", columnList = "is_active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 미션 타입: OFFICIAL(공식 미션), CUSTOM(커스텀 미션)
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false, length = 20)
    private MissionType missionType;

    // 커스텀 미션 생성자 (CUSTOM인 경우만 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // 카테고리: DAILY_LIFE(일상), GROWTH(성장), EXERCISE(운동), STUDY(학습), HEALTH(건강), RELATIONSHIP(관계)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionCategory category;

    // 인증방식: TIMER(시간인증), GPS(GPS인증), COMMUNITY(커뮤인증)
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

    // ============ 공식 미션용 사용자 맞춤 필드들 ============

    // 고민 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "worry_type", length = 20)
    private WorryType worryType;

    // 연령대 (복수 선택 가능)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "mission_age_ranges", joinColumns = @JoinColumn(name = "mission_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "age_range", length = 20)
    private List<AgeRange> ageRanges = new ArrayList<>();

    // 성별
    @Enumerated(EnumType.STRING)
    @Column(name = "gender_type", length = 10)
    private GenderType genderType;

    // 지역
    @Enumerated(EnumType.STRING)
    @Column(name = "region_type", length = 30)
    private RegionType regionType;

    // 장소
    @Enumerated(EnumType.STRING)
    @Column(name = "place_type", length = 10)
    private PlaceType placeType;

    // 난이도
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 10)
    private DifficultyLevel difficultyLevel;

    // ============ 커스텀 미션용 필드들 ============

    // 미션 기간 (일 수) - 커스텀 미션용
    @Column(name = "duration_days")
    private Integer durationDays;

    // 공개 여부 - 커스텀 미션용
    @Column(name = "is_public")
    private Boolean isPublic;

    // ============ 공식 미션 생성용 빌더 ============
    @Builder(builderMethodName = "officialBuilder")
    private Mission(String title, String description, MissionCategory category, VerificationType verificationType,
                    BigDecimal gpsLatitude, BigDecimal gpsLongitude, Integer gpsRadiusMeters,
                    Integer requiredMinutes, Integer expReward, Integer badgeDurationDays, Boolean isActive,
                    WorryType worryType, List<AgeRange> ageRanges, GenderType genderType, RegionType regionType,
                    PlaceType placeType, DifficultyLevel difficultyLevel) {
        this.missionType = MissionType.OFFICIAL;
        this.title = title;
        this.description = description;
        this.category = category;
        this.verificationType = verificationType;
        this.gpsLatitude = gpsLatitude;
        this.gpsLongitude = gpsLongitude;
        this.gpsRadiusMeters = gpsRadiusMeters != null ? gpsRadiusMeters : 100;
        this.requiredMinutes = requiredMinutes;
        this.expReward = expReward != null ? expReward : 10;
        this.badgeDurationDays = badgeDurationDays != null ? badgeDurationDays : 3;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = LocalDateTime.now();
        // 사용자 맞춤 필드
        this.worryType = worryType;
        this.ageRanges = ageRanges != null ? new ArrayList<>(ageRanges) : new ArrayList<>();
        this.genderType = genderType != null ? genderType : GenderType.ALL;
        this.regionType = regionType != null ? regionType : RegionType.ALL;
        this.placeType = placeType != null ? placeType : PlaceType.HOME;
        this.difficultyLevel = difficultyLevel != null ? difficultyLevel : DifficultyLevel.LEVEL1;
    }

    // ============ 커스텀 미션 생성용 빌더 ============
    @Builder(builderMethodName = "customBuilder")
    public static Mission createCustomMission(User creator, String title, String description, WorryType worryType,
                                               MissionCategory category, DifficultyLevel difficultyLevel, Integer durationDays,
                                               Boolean isPublic, VerificationType verificationType, BigDecimal gpsLatitude,
                                               BigDecimal gpsLongitude, Integer gpsRadiusMeters, Integer requiredMinutes,
                                               Integer expReward, Integer badgeDurationDays, Boolean isActive) {
        Mission mission = new Mission();
        mission.missionType = MissionType.CUSTOM;
        mission.creator = creator;
        mission.title = title;
        mission.description = description;
        mission.worryType = worryType;
        mission.category = category != null ? category : MissionCategory.DAILY_LIFE;
        mission.difficultyLevel = difficultyLevel != null ? difficultyLevel : DifficultyLevel.LEVEL2;
        mission.durationDays = durationDays;
        mission.isPublic = isPublic != null ? isPublic : false;
        mission.verificationType = verificationType;
        mission.gpsLatitude = gpsLatitude;
        mission.gpsLongitude = gpsLongitude;
        mission.gpsRadiusMeters = gpsRadiusMeters != null ? gpsRadiusMeters : 100;
        mission.requiredMinutes = requiredMinutes;
        mission.expReward = expReward != null ? expReward : calculateDefaultExpReward(difficultyLevel);
        mission.badgeDurationDays = badgeDurationDays != null ? badgeDurationDays : 3;
        mission.isActive = isActive != null ? isActive : true;
        mission.createdAt = LocalDateTime.now();
        return mission;
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

    // 공식 미션 업데이트
    public void updateOfficial(String title, String description, MissionCategory category, VerificationType verificationType,
                                BigDecimal gpsLatitude, BigDecimal gpsLongitude, Integer gpsRadiusMeters,
                                Integer requiredMinutes, Integer expReward, Integer badgeDurationDays,
                                WorryType worryType, List<AgeRange> ageRanges, GenderType genderType, RegionType regionType,
                                PlaceType placeType, DifficultyLevel difficultyLevel) {
        if (this.missionType != MissionType.OFFICIAL) {
            throw new IllegalStateException("공식 미션만 이 메서드로 수정할 수 있습니다.");
        }
        this.title = title;
        this.description = description;
        this.category = category;
        this.verificationType = verificationType;
        this.gpsLatitude = gpsLatitude;
        this.gpsLongitude = gpsLongitude;
        this.gpsRadiusMeters = gpsRadiusMeters != null ? gpsRadiusMeters : 100;
        this.requiredMinutes = requiredMinutes;
        this.expReward = expReward != null ? expReward : 10;
        this.badgeDurationDays = badgeDurationDays != null ? badgeDurationDays : 3;
        this.worryType = worryType;
        this.ageRanges = ageRanges != null ? new ArrayList<>(ageRanges) : new ArrayList<>();
        this.genderType = genderType != null ? genderType : GenderType.ALL;
        this.regionType = regionType != null ? regionType : RegionType.ALL;
        this.placeType = placeType != null ? placeType : PlaceType.HOME;
        this.difficultyLevel = difficultyLevel != null ? difficultyLevel : DifficultyLevel.LEVEL1;
    }

    // 커스텀 미션 업데이트
    public void updateCustom(String title, String description, WorryType worryType, MissionCategory category,
                              DifficultyLevel difficultyLevel, Integer expReward, Boolean isPublic) {
        if (this.missionType != MissionType.CUSTOM) {
            throw new IllegalStateException("커스텀 미션만 이 메서드로 수정할 수 있습니다.");
        }
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (worryType != null) this.worryType = worryType;
        if (category != null) this.category = category;
        if (difficultyLevel != null) this.difficultyLevel = difficultyLevel;
        if (expReward != null) this.expReward = expReward;
        if (isPublic != null) this.isPublic = isPublic;
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // 공식 미션 여부
    public boolean isOfficialMission() {
        return this.missionType == MissionType.OFFICIAL;
    }

    // 커스텀 미션 여부
    public boolean isCustomMission() {
        return this.missionType == MissionType.CUSTOM;
    }

    // 커스텀 미션 생성자 확인
    public boolean isCreator(Long userId) {
        if (this.missionType != MissionType.CUSTOM || this.creator == null) {
            return false;
        }
        return this.creator.getId().equals(userId);
    }
}
