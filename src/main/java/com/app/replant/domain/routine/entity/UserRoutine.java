package com.app.replant.domain.routine.entity;

import com.app.replant.domain.routine.enums.PeriodType;
import com.app.replant.domain.routine.enums.RoutineType;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "user_routine", indexes = {
    @Index(name = "idx_user_routine_user", columnList = "user_id"),
    @Index(name = "idx_user_routine_type", columnList = "routine_type"),
    @Index(name = "idx_user_routine_period", columnList = "period_type, period_start")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 루틴 유형: WAKE_UP_TIME, DAILY_PLACE, WEEKLY_RESOLUTION, MONTHLY_RESOLUTION 등
    @Enumerated(EnumType.STRING)
    @Column(name = "routine_type", nullable = false, length = 30)
    private RoutineType routineType;

    // 주기 유형: DAILY, WEEKLY, MONTHLY, NONE (일회성)
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    private PeriodType periodType;

    // 해당 주기 시작일 (예: 이번 주 월요일, 이번 달 1일)
    @Column(name = "period_start")
    private LocalDate periodStart;

    // 해당 주기 종료일
    @Column(name = "period_end")
    private LocalDate periodEnd;

    // 텍스트 값 (다짐, 장소명 등)
    @Column(name = "value_text", length = 500)
    private String valueText;

    // 시간 값 (기상시간 등)
    @Column(name = "value_time")
    private LocalTime valueTime;

    // 숫자 값 (목표 횟수 등)
    @Column(name = "value_number")
    private Integer valueNumber;

    // 위도 (장소용)
    @Column(name = "value_latitude")
    private Double valueLatitude;

    // 경도 (장소용)
    @Column(name = "value_longitude")
    private Double valueLongitude;

    // 알림 활성화 여부
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled;

    // 알림 시간 (알림 받을 시간)
    @Column(name = "notification_time")
    private LocalTime notificationTime;

    // 활성화 여부 (현재 적용 중인 설정인지)
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private UserRoutine(User user, RoutineType routineType, PeriodType periodType,
                        LocalDate periodStart, LocalDate periodEnd,
                        String valueText, LocalTime valueTime, Integer valueNumber,
                        Double valueLatitude, Double valueLongitude,
                        Boolean notificationEnabled, LocalTime notificationTime) {
        this.user = user;
        this.routineType = routineType;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.valueText = valueText;
        this.valueTime = valueTime;
        this.valueNumber = valueNumber;
        this.valueLatitude = valueLatitude;
        this.valueLongitude = valueLongitude;
        this.notificationEnabled = notificationEnabled != null ? notificationEnabled : false;
        this.notificationTime = notificationTime;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String valueText, LocalTime valueTime, Integer valueNumber,
                       Double valueLatitude, Double valueLongitude,
                       Boolean notificationEnabled, LocalTime notificationTime) {
        if (valueText != null) this.valueText = valueText;
        if (valueTime != null) this.valueTime = valueTime;
        if (valueNumber != null) this.valueNumber = valueNumber;
        if (valueLatitude != null) this.valueLatitude = valueLatitude;
        if (valueLongitude != null) this.valueLongitude = valueLongitude;
        if (notificationEnabled != null) this.notificationEnabled = notificationEnabled;
        if (notificationTime != null) this.notificationTime = notificationTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }
}
