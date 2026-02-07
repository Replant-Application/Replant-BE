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

    @NotBlank(message = "기상 시간을 입력해주세요")
    @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "기상 시간은 HH:mm 형식이어야 합니다 (예: 07:00)")
    private String wakeTime;
}
