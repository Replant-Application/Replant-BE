package com.app.replant.domain.usermission.dto;

import com.app.replant.domain.usermission.enums.WakeupTimeSlot;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WakeupMissionSettingRequest {

    @NotNull(message = "시간대를 선택해주세요")
    private WakeupTimeSlot timeSlot;

    @NotNull(message = "주차를 입력해주세요")
    private Integer targetWeekNumber;

    @NotNull(message = "연도를 입력해주세요")
    private Integer targetYear;
}
