package com.app.replant.domain.mission.entity;

import com.app.replant.domain.mission.enums.*;
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

    // 기간: DAILY(일간), WEEKLY(주간), MONTHLY(월간)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionType type;

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

    // ============ 사용자 맞춤 필드들 ============

    // 고민 종류: RE_EMPLOYMENT(재취업), JOB_PREPARATION(취업준비), ENTRANCE_EXAM(입시),
    //          ADVANCEMENT(진학), RETURN_TO_SCHOOL(복학), RELATIONSHIP(연애), SELF_MANAGEMENT(자기관리)
    @Enumerated(EnumType.STRING)
    @Column(name = "worry_type", length = 20)
    private WorryType worryType;

    // 연령대 (복수 선택 가능)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "mission_age_ranges", joinColumns = @JoinColumn(name = "mission_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "age_range", length = 20)
    private List<AgeRange> ageRanges = new ArrayList<>();

    // 성별: MALE(남성), FEMALE(여성), ALL(전체)
    @Enumerated(EnumType.STRING)
    @Column(name = "gender_type", length = 10)
    private GenderType genderType;

    // 지역: 광역자치단체 단위
    @Enumerated(EnumType.STRING)
    @Column(name = "region_type", length = 30)
    private RegionType regionType;

    // 장소: HOME(집), OUTDOOR(야외), INDOOR(실내)
    @Enumerated(EnumType.STRING)
    @Column(name = "place_type", length = 10)
    private PlaceType placeType;

    // 난이도: LEVEL1, LEVEL2, LEVEL3
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 10)
    private DifficultyLevel difficultyLevel;

    @Builder
    private Mission(String title, String description, MissionType type, VerificationType verificationType,
                    BigDecimal gpsLatitude, BigDecimal gpsLongitude, Integer gpsRadiusMeters,
                    Integer requiredMinutes, Integer expReward, Integer badgeDurationDays, Boolean isActive,
                    WorryType worryType, List<AgeRange> ageRanges, GenderType genderType, RegionType regionType,
                    PlaceType placeType, DifficultyLevel difficultyLevel) {
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
        // 사용자 맞춤 필드
        this.worryType = worryType;
        this.ageRanges = ageRanges != null ? new ArrayList<>(ageRanges) : new ArrayList<>();
        this.genderType = genderType != null ? genderType : GenderType.ALL;
        this.regionType = regionType != null ? regionType : RegionType.ALL;
        this.placeType = placeType != null ? placeType : PlaceType.HOME;
        this.difficultyLevel = difficultyLevel != null ? difficultyLevel : DifficultyLevel.LEVEL1;
    }

    public void update(String title, String description, MissionType type, VerificationType verificationType,
                       BigDecimal gpsLatitude, BigDecimal gpsLongitude, Integer gpsRadiusMeters,
                       Integer requiredMinutes, Integer expReward, Integer badgeDurationDays,
                       WorryType worryType, List<AgeRange> ageRanges, GenderType genderType, RegionType regionType,
                       PlaceType placeType, DifficultyLevel difficultyLevel) {
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
        // 사용자 맞춤 필드
        this.worryType = worryType;
        this.ageRanges = ageRanges != null ? new ArrayList<>(ageRanges) : new ArrayList<>();
        this.genderType = genderType != null ? genderType : GenderType.ALL;
        this.regionType = regionType != null ? regionType : RegionType.ALL;
        this.placeType = placeType != null ? placeType : PlaceType.HOME;
        this.difficultyLevel = difficultyLevel != null ? difficultyLevel : DifficultyLevel.LEVEL1;
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
