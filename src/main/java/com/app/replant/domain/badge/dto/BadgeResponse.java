package com.app.replant.domain.badge.dto;

import com.app.replant.domain.badge.entity.UserBadge;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class BadgeResponse {

    private Long id;
    private String missionType; // "SYSTEM" or "CUSTOM"
    private MissionInfo mission;
    private CustomMissionInfo customMission;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private Long remainingDays;
    private Boolean isExpired;

    @Getter
    @Builder
    public static class MissionInfo {
        private Long id;
        private String title;
    }

    @Getter
    @Builder
    public static class CustomMissionInfo {
        private Long id;
        private String title;
    }

    public static BadgeResponse from(UserBadge userBadge) {
        boolean isExpired = userBadge.isExpired();
        long remainingDays = 0;
        if (!isExpired) {
            remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), userBadge.getExpiresAt());
        }

        BadgeResponseBuilder builder = BadgeResponse.builder()
                .id(userBadge.getId())
                .issuedAt(userBadge.getIssuedAt())
                .expiresAt(userBadge.getExpiresAt())
                .remainingDays(remainingDays)
                .isExpired(isExpired);

        if (userBadge.getMission() != null) {
            if (userBadge.getMission().isOfficialMission()) {
                builder.missionType("SYSTEM")
                        .mission(MissionInfo.builder()
                                .id(userBadge.getMission().getId())
                                .title(userBadge.getMission().getTitle())
                                .build());
            } else {
                builder.missionType("CUSTOM")
                        .customMission(CustomMissionInfo.builder()
                                .id(userBadge.getMission().getId())
                                .title(userBadge.getMission().getTitle())
                                .build());
            }
        }

        return builder.build();
    }
}
