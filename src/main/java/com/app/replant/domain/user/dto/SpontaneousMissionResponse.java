package com.app.replant.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpontaneousMissionResponse {
    private Boolean isSpontaneousMissionSetupCompleted;
    private String sleepTime;
    private String wakeTime;
    private String breakfastTime;
    private String lunchTime;
    private String dinnerTime;
}
