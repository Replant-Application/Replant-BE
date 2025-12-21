package com.app.replant.domain.mission.dto;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MissionResponse {
    private Long id;
    private String title;
    private String description;
    private MissionType type;
    private VerificationType verificationType;
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
    private Integer gpsRadiusMeters;
    private Integer requiredMinutes;
    private Integer expReward;
    private Integer badgeDurationDays;
    private Long reviewCount;
    private Long qnaCount;

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
                .build();
    }

    public static MissionResponse from(Mission mission, long reviewCount, long qnaCount) {
        MissionResponse response = from(mission);
        return MissionResponse.builder()
                .id(response.id)
                .title(response.title)
                .description(response.description)
                .type(response.type)
                .verificationType(response.verificationType)
                .gpsLatitude(response.gpsLatitude)
                .gpsLongitude(response.gpsLongitude)
                .gpsRadiusMeters(response.gpsRadiusMeters)
                .requiredMinutes(response.requiredMinutes)
                .expReward(response.expReward)
                .badgeDurationDays(response.badgeDurationDays)
                .reviewCount(reviewCount)
                .qnaCount(qnaCount)
                .build();
    }
}
