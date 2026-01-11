package com.app.replant.domain.routine.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoutineType {
    // 기상 관련
    WAKE_UP_TIME("기상 시간", PeriodType.DAILY, "time", "기상 미션용 시간대 설정"),

    // 공부 시간 관련 (시작~종료)
    STUDY_TIME("공부 시간", PeriodType.DAILY, "time_range", "공부 미션용 시간대 설정"),

    // 장소 관련
    DAILY_PLACE("매일 갈 장소", PeriodType.DAILY, "place", "매일 방문할 장소"),
    GYM_LOCATION("헬스장", PeriodType.NONE, "place", "헬스장 방문 미션용"),
    LIBRARY_LOCATION("도서관", PeriodType.NONE, "place", "도서관 방문 미션용"),
    CUSTOM_LOCATION("기타 장소", PeriodType.NONE, "place", "기타 장소 방문 미션용"),

    // 다짐 관련
    WEEKLY_RESOLUTION("이번 주 다짐", PeriodType.WEEKLY, "text", "이번 주 목표"),
    MONTHLY_RESOLUTION("이번 달 다짐", PeriodType.MONTHLY, "text", "이번 달 목표"),

    // 운동 관련
    EXERCISE_TARGET("운동 목표", PeriodType.WEEKLY, "number", "주간 운동 횟수 목표"),

    // 학습 관련
    STUDY_TARGET("학습 목표", PeriodType.WEEKLY, "number", "주간 학습 시간 목표"),

    // 기타 확장 가능
    CUSTOM("사용자 정의", PeriodType.NONE, "text", "사용자 정의 루틴");

    private final String displayName;
    private final PeriodType defaultPeriodType;
    private final String inputType;  // time, time_range, place, text, number
    private final String defaultDescription;
}
