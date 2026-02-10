package com.app.replant.domain.mission.dto;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class MissionResponse {
    private Long id;
    private MissionType missionType;  // OFFICIAL or CUSTOM
    private String title;
    private String description;
    // 카테고리: DAILY_LIFE(일상), GROWTH(성장), EXERCISE(운동), STUDY(학습), HEALTH(건강), RELATIONSHIP(관계)
    private MissionCategory category;
    // 인증방식: TIMER(시간인증), COMMUNITY(커뮤인증)
    private VerificationType verificationType;
    private Integer requiredMinutes;
    private Integer expReward;
    private Integer badgeDurationDays;
    private Boolean isActive;
    private Long reviewCount;
    
    // 미션 도감용: 사용자가 해당 미션을 수행했는지 여부 (UserMission이 존재하면 true)
    @lombok.Builder.Default
    private Boolean isAttempted = false;
    
    // 미션 도감용: 사용자가 해당 미션을 완료했는지 여부 (인증 완료 = 잠금 해제)
    @lombok.Builder.Default
    private Boolean isCompleted = false;
    
    // 미션 참여자 수 (해당 미션을 수행한 고유 사용자 수)
    private Long participantCount;

    // ============ 사용자 맞춤 필드들 ============
    // 고민 종류
    private WorryType worryType;
    // 연령대 (복수 선택 가능)
    private List<AgeRange> ageRanges;
    // 성별
    private GenderType genderType;
    // 지역
    private RegionType regionType;
    // 장소
    private PlaceType placeType;
    // 난이도
    private DifficultyLevel difficultyLevel;

    // ============ 커스텀 미션 전용 필드들 ============
    private Long creatorId;
    private String creatorNickname;
    private Integer durationDays;
    private Boolean isPublic;
    private Integer deadlineDays;
    private Boolean isPromoted;

    public static MissionResponse from(Mission mission) {
        return from(mission, false, false);  // 기본값 false
    }

    public static MissionResponse from(Mission mission, boolean isCompleted) {
        return from(mission, false, isCompleted);  // isAttempted 기본값 false
    }

    public static MissionResponse from(Mission mission, boolean isAttempted, boolean isCompleted) {
        return from(mission, isAttempted, isCompleted, null);
    }

    public static MissionResponse from(Mission mission, boolean isAttempted, boolean isCompleted, Long participantCount) {
        // ageRanges를 안전하게 복사 (LazyInitializationException 방지)
        List<AgeRange> ageRanges = new ArrayList<>();
        try {
            List<AgeRange> originalAgeRanges = mission.getAgeRanges();
            if (originalAgeRanges != null) {
                // 명시적으로 접근하여 초기화 시도 (세션이 열려있으면 초기화됨)
                ageRanges = new ArrayList<>(originalAgeRanges);
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // 세션이 닫혀있으면 빈 리스트로 설정
            ageRanges = new ArrayList<>();
        }
        
        MissionResponseBuilder builder = MissionResponse.builder()
                .id(mission.getId())
                .missionType(mission.getMissionType())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .category(mission.getCategory())
                .verificationType(mission.getVerificationType())
                .requiredMinutes(mission.getRequiredMinutes())
                .expReward(mission.isCustomMission() ? 0 : mission.getExpReward())
                .badgeDurationDays(mission.getBadgeDurationDays())
                .isActive(mission.getIsActive())
                .isAttempted(isAttempted)
                .isCompleted(isCompleted)
                .participantCount(participantCount)
                // 사용자 맞춤 필드
                .worryType(mission.getWorryType())
                .ageRanges(ageRanges)
                .genderType(mission.getGenderType())
                .regionType(mission.getRegionType())
                .placeType(mission.getPlaceType())
                .difficultyLevel(mission.getDifficultyLevel());

        // 커스텀 미션 전용 필드
        if (mission.isCustomMission()) {
            builder.creatorId(mission.getCreator() != null ? mission.getCreator().getId() : null)
                    .creatorNickname(mission.getCreator() != null ? mission.getCreator().getNickname() : null)
                    .durationDays(mission.getDurationDays())
                    .isPublic(mission.getIsPublic())
                    .deadlineDays(mission.getDeadlineDays())
                    .isPromoted(mission.getIsPromoted());
        }

        return builder.build();
    }

    public static MissionResponse from(Mission mission, long reviewCount) {
        return from(mission, reviewCount, false, false);
    }

    public static MissionResponse from(Mission mission, long reviewCount, boolean isCompleted) {
        return from(mission, reviewCount, false, isCompleted);  // isAttempted 기본값 false
    }

    public static MissionResponse from(Mission mission, long reviewCount, boolean isAttempted, boolean isCompleted) {
        return from(mission, reviewCount, isAttempted, isCompleted, null);
    }

    public static MissionResponse from(Mission mission, long reviewCount, boolean isAttempted, boolean isCompleted, Long participantCount) {
        // ageRanges를 안전하게 복사 (LazyInitializationException 방지)
        List<AgeRange> ageRanges = new ArrayList<>();
        try {
            List<AgeRange> originalAgeRanges = mission.getAgeRanges();
            if (originalAgeRanges != null) {
                // 명시적으로 접근하여 초기화 시도 (세션이 열려있으면 초기화됨)
                ageRanges = new ArrayList<>(originalAgeRanges);
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // 세션이 닫혀있으면 빈 리스트로 설정
            ageRanges = new ArrayList<>();
        }
        
        MissionResponseBuilder builder = MissionResponse.builder()
                .id(mission.getId())
                .missionType(mission.getMissionType())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .category(mission.getCategory())
                .verificationType(mission.getVerificationType())
                .requiredMinutes(mission.getRequiredMinutes())
                .expReward(mission.isCustomMission() ? 0 : mission.getExpReward())
                .badgeDurationDays(mission.getBadgeDurationDays())
                .isActive(mission.getIsActive())
                .reviewCount(reviewCount)
                .isAttempted(isAttempted)
                .isCompleted(isCompleted)
                .participantCount(participantCount)
                // 사용자 맞춤 필드
                .worryType(mission.getWorryType())
                .ageRanges(ageRanges)
                .genderType(mission.getGenderType())
                .regionType(mission.getRegionType())
                .placeType(mission.getPlaceType())
                .difficultyLevel(mission.getDifficultyLevel());

        // 커스텀 미션 전용 필드
        if (mission.isCustomMission()) {
            builder.creatorId(mission.getCreator() != null ? mission.getCreator().getId() : null)
                    .creatorNickname(mission.getCreator() != null ? mission.getCreator().getNickname() : null)
                    .durationDays(mission.getDurationDays())
                    .isPublic(mission.getIsPublic())
                    .deadlineDays(mission.getDeadlineDays())
                    .isPromoted(mission.getIsPromoted());
        }

        return builder.build();
    }
}
