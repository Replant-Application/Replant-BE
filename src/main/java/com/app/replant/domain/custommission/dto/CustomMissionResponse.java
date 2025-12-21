package com.app.replant.domain.custommission.dto;

import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.mission.enums.VerificationType;
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
