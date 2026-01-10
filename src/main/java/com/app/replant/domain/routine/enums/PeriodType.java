package com.app.replant.domain.routine.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PeriodType {
    DAILY("매일"),
    WEEKLY("매주"),
    MONTHLY("매월"),
    NONE("상시");

    private final String displayName;
}
