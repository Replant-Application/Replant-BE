package com.app.replant.domain.usermission.dto;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionCategory;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserMissionResponse {

    private Long id;
    private String missionType; // OFFICIAL or CUSTOM
    private MissionInfo mission;
    private CustomMissionInfo customMission;
    private LocalDateTime assignedAt;
    private LocalDateTime dueDate;
    private UserMissionStatus status;
    private LocalDateTime completedAt; // 완료 날짜

    @Getter
    @Builder
    public static class MissionInfo {
        private Long id;
        private String title;
        private MissionCategory category;
        private VerificationType verificationType;
        private Integer requiredMinutes;
    }

    @Getter
    @Builder
    public static class CustomMissionInfo {
        private Long id;
        private String title;
        private Integer durationDays;
        private VerificationType verificationType;
    }

    public static UserMissionResponse from(UserMission userMission) {
        return from(userMission, null);
    }

    public static UserMissionResponse from(UserMission userMission, LocalDateTime completedAt) {
        UserMissionResponseBuilder builder = UserMissionResponse.builder()
                .id(userMission.getId())
                .assignedAt(userMission.getAssignedAt())
                .dueDate(userMission.getDueDate())
                .status(userMission.getStatus())
                .completedAt(completedAt);

        // 통합된 Mission 엔티티 사용
        Mission mission = userMission.getMission();
        if (mission != null) {
            if (mission.isOfficialMission()) {
                builder.missionType("OFFICIAL")
                        .mission(MissionInfo.builder()
                                .id(mission.getId())
                                .title(mission.getTitle())
                                .category(mission.getCategory())
                                .verificationType(mission.getVerificationType())
                                .requiredMinutes(mission.getRequiredMinutes())
                                .build());
            } else {
                // 커스텀 미션의 경우
                builder.missionType("CUSTOM")
                        .customMission(CustomMissionInfo.builder()
                                .id(mission.getId())
                                .title(mission.getTitle())
                                .durationDays(mission.getDurationDays())
                                .verificationType(mission.getVerificationType())
                                .build());
            }
        }

        return builder.build();
    }
}
