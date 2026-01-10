package com.app.replant.domain.routine.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoutineType {
    // 기상 관련
    WAKE_UP_TIME("기상 시간", PeriodType.DAILY),

    // 장소 관련
    DAILY_PLACE("매일 갈 장소", PeriodType.DAILY),

    // 다짐 관련
    WEEKLY_RESOLUTION("이번 주 다짐", PeriodType.WEEKLY),
    MONTHLY_RESOLUTION("이번 달 다짐", PeriodType.MONTHLY),

    // 운동 관련
    EXERCISE_TARGET("운동 목표", PeriodType.WEEKLY),

    // 학습 관련
    STUDY_TARGET("학습 목표", PeriodType.WEEKLY),

    // 기타 확장 가능
    CUSTOM("사용자 정의", PeriodType.NONE);

    private final String displayName;
    private final PeriodType defaultPeriodType;
}
