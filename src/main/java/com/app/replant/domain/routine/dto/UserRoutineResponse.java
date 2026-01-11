package com.app.replant.domain.routine.dto;

import com.app.replant.domain.routine.entity.UserRoutine;
import com.app.replant.domain.routine.enums.PeriodType;
import com.app.replant.domain.routine.enums.RoutineType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class UserRoutineResponse {

    private Long id;
    private RoutineType routineType;
    private String routineTypeName;  // 표시용 이름
    private String inputType;        // time, time_range, place, text, number
    private String defaultDescription;
    private PeriodType periodType;
    private String periodTypeName;   // 표시용 이름
    private LocalDate periodStart;
    private LocalDate periodEnd;

    // 루틴 제목 및 설명
    private String title;
    private String description;

    // 값들
    private String valueText;
    private LocalTime valueTimeStart;
    private LocalTime valueTimeEnd;
    private LocalTime valueTime;  // 기존 호환용
    private Integer valueNumber;
    private Double valueLatitude;
    private Double valueLongitude;

    // 알림 설정
    private Boolean notificationEnabled;
    private LocalTime notificationTime;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserRoutineResponse from(UserRoutine routine) {
        return UserRoutineResponse.builder()
                .id(routine.getId())
                .routineType(routine.getRoutineType())
                .routineTypeName(routine.getRoutineType().getDisplayName())
                .inputType(routine.getRoutineType().getInputType())
                .defaultDescription(routine.getRoutineType().getDefaultDescription())
                .periodType(routine.getPeriodType())
                .periodTypeName(routine.getPeriodType().getDisplayName())
                .periodStart(routine.getPeriodStart())
                .periodEnd(routine.getPeriodEnd())
                .title(routine.getTitle())
                .description(routine.getDescription())
                .valueText(routine.getValueText())
                .valueTimeStart(routine.getValueTimeStart())
                .valueTimeEnd(routine.getValueTimeEnd())
                .valueTime(routine.getValueTime())
                .valueNumber(routine.getValueNumber())
                .valueLatitude(routine.getValueLatitude())
                .valueLongitude(routine.getValueLongitude())
                .notificationEnabled(routine.getNotificationEnabled())
                .notificationTime(routine.getNotificationTime())
                .isActive(routine.getIsActive())
                .createdAt(routine.getCreatedAt())
                .updatedAt(routine.getUpdatedAt())
                .build();
    }
}
