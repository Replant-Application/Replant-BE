package com.app.replant.domain.mission.dto;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import lombok.Builder;
import lombok.Getter;

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
    private Long qnaCount;

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
    private Boolean isChallenge;
    private Integer challengeDays;
    private Integer deadlineDays;
    private Boolean isPromoted;

    public static MissionResponse from(Mission mission) {
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
                // 사용자 맞춤 필드
                .worryType(mission.getWorryType())
                .ageRanges(mission.getAgeRanges())
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
                    .isChallenge(mission.getIsChallenge())
                    .challengeDays(mission.getChallengeDays())
                    .deadlineDays(mission.getDeadlineDays())
                    .isPromoted(mission.getIsPromoted());
        }

        return builder.build();
    }

    public static MissionResponse from(Mission mission, long reviewCount, long qnaCount) {
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
                .qnaCount(qnaCount)
                // 사용자 맞춤 필드
                .worryType(mission.getWorryType())
                .ageRanges(mission.getAgeRanges())
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
                    .isChallenge(mission.getIsChallenge())
                    .challengeDays(mission.getChallengeDays())
                    .deadlineDays(mission.getDeadlineDays())
                    .isPromoted(mission.getIsPromoted());
        }

        return builder.build();
    }
}
