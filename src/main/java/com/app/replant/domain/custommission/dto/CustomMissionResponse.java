package com.app.replant.domain.custommission.dto;

import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.mission.enums.DifficultyLevel;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.enums.WorryType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CustomMissionResponse {

    private Long id;
    private String title;
    private String description;
    private Long creatorId;
    private String creatorNickname;

    // 새로 추가된 필드들 (시스템 미션과 통합)
    private WorryType worryType;           // 고민 종류
    private MissionType missionType;        // 미션 타입 (카테고리): DAILY_LIFE, GROWTH, EXERCISE 등
    private DifficultyLevel difficultyLevel; // 난이도 (EASY, MEDIUM, HARD)

    private Integer challengeDays;         // 챌린지 기간 (일수)
    private Integer deadlineDays;          // 완료 기한 (일수)
    private Integer durationDays;
    private Boolean isPublic;
    private VerificationType verificationType;
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
    private Integer gpsRadiusMeters;
    private Integer requiredMinutes;
    private Integer expReward;
    private Integer badgeDurationDays;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static CustomMissionResponse from(CustomMission customMission) {
        return CustomMissionResponse.builder()
                .id(customMission.getId())
                .title(customMission.getTitle())
                .description(customMission.getDescription())
                .creatorId(customMission.getCreator().getId())
                .creatorNickname(customMission.getCreator().getNickname())
                .worryType(customMission.getWorryType())
                .missionType(customMission.getMissionType())
                .difficultyLevel(customMission.getDifficultyLevel())
                .challengeDays(customMission.getChallengeDays())
                .deadlineDays(customMission.getDeadlineDays())
                .durationDays(customMission.getDurationDays())
                .isPublic(customMission.getIsPublic())
                .verificationType(customMission.getVerificationType())
                .gpsLatitude(customMission.getGpsLatitude())
                .gpsLongitude(customMission.getGpsLongitude())
                .gpsRadiusMeters(customMission.getGpsRadiusMeters())
                .requiredMinutes(customMission.getRequiredMinutes())
                .expReward(customMission.getExpReward())
                .badgeDurationDays(customMission.getBadgeDurationDays())
                .isActive(customMission.getIsActive())
                .createdAt(customMission.getCreatedAt())
                .build();
    }
}
