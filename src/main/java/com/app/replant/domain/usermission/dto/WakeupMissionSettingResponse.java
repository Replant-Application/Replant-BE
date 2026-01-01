package com.app.replant.domain.usermission.dto;

import com.app.replant.domain.usermission.entity.WakeupMissionSetting;
import com.app.replant.domain.usermission.enums.WakeupTimeSlot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WakeupMissionSettingResponse {

    private Long id;
    private WakeupTimeSlot timeSlot;
    private String timeSlotDisplay;
    private Integer startHour;
    private Integer endHour;
    private Integer weekNumber;
    private Integer year;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WakeupMissionSettingResponse from(WakeupMissionSetting setting) {
        return WakeupMissionSettingResponse.builder()
                .id(setting.getId())
                .timeSlot(setting.getTimeSlot())
                .timeSlotDisplay(setting.getTimeSlot().getDisplayName())
                .startHour(setting.getTimeSlot().getStartHour())
                .endHour(setting.getTimeSlot().getEndHour())
                .weekNumber(setting.getWeekNumber())
                .year(setting.getYear())
                .isActive(setting.getIsActive())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
