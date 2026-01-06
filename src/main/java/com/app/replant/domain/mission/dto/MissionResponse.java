package com.app.replant.domain.mission.dto;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class MissionResponse {
    private Long id;
    private String title;
    private String description;
    // 기간: DAILY(일간), WEEKLY(주간), MONTHLY(월간)
    private MissionType type;
    // 인증방식: TIMER(시간인증), GPS(GPS인증), COMMUNITY(커뮤인증)
    private VerificationType verificationType;
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
    private Integer gpsRadiusMeters;
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

    public static MissionResponse from(Mission mission) {
        return MissionResponse.builder()
                .id(mission.getId())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .type(mission.getType())
                .verificationType(mission.getVerificationType())
                .gpsLatitude(mission.getGpsLatitude())
                .gpsLongitude(mission.getGpsLongitude())
                .gpsRadiusMeters(mission.getGpsRadiusMeters())
                .requiredMinutes(mission.getRequiredMinutes())
                .expReward(mission.getExpReward())
                .badgeDurationDays(mission.getBadgeDurationDays())
                .isActive(mission.getIsActive())
                // 사용자 맞춤 필드
                .worryType(mission.getWorryType())
                .ageRanges(mission.getAgeRanges())
                .genderType(mission.getGenderType())
                .regionType(mission.getRegionType())
                .placeType(mission.getPlaceType())
                .difficultyLevel(mission.getDifficultyLevel())
                .build();
    }

    public static MissionResponse from(Mission mission, long reviewCount, long qnaCount) {
        return MissionResponse.builder()
                .id(mission.getId())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .type(mission.getType())
                .verificationType(mission.getVerificationType())
                .gpsLatitude(mission.getGpsLatitude())
                .gpsLongitude(mission.getGpsLongitude())
                .gpsRadiusMeters(mission.getGpsRadiusMeters())
                .requiredMinutes(mission.getRequiredMinutes())
                .expReward(mission.getExpReward())
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
                .difficultyLevel(mission.getDifficultyLevel())
                .build();
    }
}
