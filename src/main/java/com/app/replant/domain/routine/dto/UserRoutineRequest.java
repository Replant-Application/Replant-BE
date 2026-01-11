package com.app.replant.domain.routine.dto;

import com.app.replant.domain.routine.enums.PeriodType;
import com.app.replant.domain.routine.enums.RoutineType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoutineRequest {

    @NotNull(message = "루틴 타입을 선택해주세요")
    private RoutineType routineType;

    // 주기 타입 (기본값은 routineType의 defaultPeriodType 사용)
    private PeriodType periodType;

    // 주기 시작일 (null이면 자동 계산)
    private LocalDate periodStart;

    // 루틴 제목 (사용자 지정 이름: 예 - "우리동네 헬스장")
    private String title;

    // 루틴 설명 (예 - "헬스장 방문 미션용")
    private String description;

    // 텍스트 값 (다짐, 장소명 등)
    private String valueText;

    // 시작 시간 (기상시간, 공부시작시간 등)
    private LocalTime valueTimeStart;

    // 종료 시간 (공부종료시간 등)
    private LocalTime valueTimeEnd;

    // 시간 값 (기존 호환용 - deprecated)
    private LocalTime valueTime;

    // 숫자 값 (목표 횟수 등)
    private Integer valueNumber;

    // 위도 (장소용)
    private Double valueLatitude;

    // 경도 (장소용)
    private Double valueLongitude;

    // 알림 설정
    private Boolean notificationEnabled;
    private LocalTime notificationTime;
}
