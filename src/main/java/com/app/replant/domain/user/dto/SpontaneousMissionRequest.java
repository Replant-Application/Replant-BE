package com.app.replant.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpontaneousMissionRequest {

    @NotBlank(message = "취침 시간을 입력해주세요")
    @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "취침 시간은 HH:mm 형식이어야 합니다 (예: 23:00)")
    private String sleepTime;

    @NotBlank(message = "기상 시간을 입력해주세요")
    @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "기상 시간은 HH:mm 형식이어야 합니다 (예: 07:00)")
    private String wakeTime;
    
    // 식사 시간 (null 가능 - 해당 식사를 안 먹는 경우)
    @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "아침 식사 시간은 HH:mm 형식이어야 합니다 (예: 08:00)")
    private String breakfastTime;
    
    @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "점심 식사 시간은 HH:mm 형식이어야 합니다 (예: 12:30)")
    private String lunchTime;
    
    @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "저녁 식사 시간은 HH:mm 형식이어야 합니다 (예: 19:00)")
    private String dinnerTime;
}
