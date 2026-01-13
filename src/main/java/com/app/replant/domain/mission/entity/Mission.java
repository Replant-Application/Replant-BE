package com.app.replant.domain.mission.entity;

import com.app.replant.domain.mission.enums.*;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mission", indexes = {
    @Index(name = "idx_mission_type", columnList = "mission_type"),
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

    // 인증방식: TIMER(시간인증), COMMUNITY(커뮤인증)
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 20)
    private VerificationType verificationType;

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

    // 챌린지 미션 여부 (true: 챌린지 미션, false: 일반 미션)
    @Column(name = "is_challenge")
    private Boolean isChallenge;

    // 챌린지 기간 (일수) - 챌린지 미션일 때만 사용
    @Column(name = "challenge_days")
    private Integer challengeDays;

    // 완료 기한 (일수) - 일반 미션일 때만 사용
    @Column(name = "deadline_days")
    private Integer deadlineDays;

    // 공식 미션으로 승격 여부 (별점/평가 기반)
    @Column(name = "is_promoted")
    private Boolean isPromoted;

    // ============ 공식 미션 생성용 빌더 ============
    @Builder(builderMethodName = "officialBuilder")
    private Mission(String title, String description, MissionCategory category, VerificationType verificationType,
                    Integer requiredMinutes, Integer expReward, Integer badgeDurationDays, Boolean isActive,
                    WorryType worryType, List<AgeRange> ageRanges, GenderType genderType, RegionType regionType,
                    PlaceType placeType, DifficultyLevel difficultyLevel) {
        this.missionType = MissionType.OFFICIAL;
        this.title = title;
        this.description = description;
        this.category = category;
        this.verificationType = verificationType;
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

    /**
     * 커스텀 미션 생성용 Static Factory Method
     * 
     * 커스텀 미션은 경험치를 지급하지 않으므로 expReward는 항상 0으로 설정됩니다.
     * 
     * @param expReward 경험치 보상 (무시됨, 항상 0으로 설정)
     * @return 생성된 커스텀 미션
     */
    public static Mission createCustomMission(User creator, String title, String description, WorryType worryType,
                                               MissionCategory category, DifficultyLevel difficultyLevel,
                                               Boolean isChallenge, Integer challengeDays, Integer deadlineDays,
                                               Integer durationDays, Boolean isPublic, VerificationType verificationType,
                                               Integer requiredMinutes, String startTime, String endTime,
                                               Integer expReward, Integer badgeDurationDays,
                                               Boolean isActive) {
        Mission mission = new Mission();
        // 커스텀 미션이므로 반드시 CUSTOM으로 설정
        mission.missionType = MissionType.CUSTOM;
        mission.creator = creator;
        mission.title = title;
        mission.description = description;
        mission.worryType = worryType;
        mission.category = category != null ? category : MissionCategory.DAILY_LIFE;
        mission.difficultyLevel = difficultyLevel != null ? difficultyLevel : DifficultyLevel.LEVEL2;
        // 챌린지 미션 관련 필드
        mission.isChallenge = isChallenge != null ? isChallenge : false;
        mission.challengeDays = mission.isChallenge ? (challengeDays != null ? challengeDays : 7) : null;
        mission.deadlineDays = mission.isChallenge ? null : (deadlineDays != null ? deadlineDays : 3);
        mission.durationDays = durationDays;
        mission.isPublic = isPublic != null ? isPublic : false;
        mission.isPromoted = false;  // 기본값: 승격되지 않음
        mission.verificationType = verificationType;
        mission.requiredMinutes = requiredMinutes;
        mission.startTime = startTime;
        mission.endTime = endTime;
        // 커스텀 미션은 항상 경험치 0으로 설정
        mission.expReward = 0;
        mission.badgeDurationDays = badgeDurationDays != null ? badgeDurationDays : 3;
        mission.isActive = isActive != null ? isActive : true;
        mission.createdAt = LocalDateTime.now();
        return mission;
    }

    // 하위 호환성을 위한 빌더 (Deprecated)
    @Deprecated
    @Builder(builderMethodName = "customBuilder")
    private static Mission customBuilderMethod(User creator, String title, String description, WorryType worryType,
                                                MissionCategory category, DifficultyLevel difficultyLevel,
                                                Boolean isChallenge, Integer challengeDays, Integer deadlineDays,
                                                Integer durationDays, Boolean isPublic, VerificationType verificationType,
                                                Integer requiredMinutes, String startTime, String endTime,
                                                Integer expReward, Integer badgeDurationDays, Boolean isActive) {
        return createCustomMission(creator, title, description, worryType, category, difficultyLevel,
                isChallenge, challengeDays, deadlineDays, durationDays, isPublic, verificationType,
                requiredMinutes, startTime, endTime, expReward, badgeDurationDays, isActive);
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

    /**
     * 커스텀 미션 업데이트
     * 
     * 커스텀 미션은 경험치를 지급하지 않으므로 expReward 파라미터는 무시되고 항상 0으로 유지됩니다.
     * 
     * @param expReward 경험치 보상 (무시됨, 항상 0으로 유지)
     */
    public void updateCustom(String title, String description, WorryType worryType, MissionCategory category,
                              DifficultyLevel difficultyLevel, Boolean isChallenge, Integer challengeDays,
                              Integer deadlineDays, Integer expReward, Boolean isPublic) {
        if (this.missionType != MissionType.CUSTOM) {
            throw new IllegalStateException("커스텀 미션만 이 메서드로 수정할 수 있습니다.");
        }
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (worryType != null) this.worryType = worryType;
        if (category != null) this.category = category;
        if (difficultyLevel != null) this.difficultyLevel = difficultyLevel;
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
        // 커스텀 미션의 경험치는 항상 0으로 유지
        this.expReward = 0;
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

    /**
     * 완료기한(deadlineDays)에 따른 뱃지 유효기간 계산
     * - 1일 → 7일
     * - 3일 → 10일
     * - 7일 → 14일
     * - 15일 → 15일
     * - 30일 → 30일
     * - 기타 → badgeDurationDays 필드값 또는 기본값 3일
     */
    public int calculateBadgeDuration() {
        // 명시적으로 설정된 badgeDurationDays가 있으면 그대로 사용
        if (this.badgeDurationDays != null && this.badgeDurationDays > 0) {
            return this.badgeDurationDays;
        }

        // deadlineDays 또는 challengeDays 기반으로 계산
        Integer days = this.deadlineDays != null ? this.deadlineDays :
                       (this.challengeDays != null ? this.challengeDays : null);

        if (days == null) {
            return 3; // 기본값
        }

        // 완료기한에 따른 뱃지 유효기간 매핑
        return switch (days) {
            case 1 -> 7;
            case 3 -> 10;
            case 7 -> 14;
            case 15 -> 15;
            case 30 -> 30;
            default -> Math.max(days, 3); // 기타 경우 기한과 동일하되 최소 3일
        };
    }
}
