package com.app.replant.domain.usermission.entity;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.usermission.enums.WakeupTimeSlot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기상 미션 시간 설정
 * 사용자가 1주차에 2주차 기상미션 시간대를 설정
 */
@Entity
@Table(name = "wakeup_mission_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WakeupMissionSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot", nullable = false, length = 20)
    private WakeupTimeSlot timeSlot;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public WakeupMissionSetting(User user, WakeupTimeSlot timeSlot, Integer weekNumber, Integer year) {
        this.user = user;
        this.timeSlot = timeSlot;
        this.weekNumber = weekNumber;
        this.year = year;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public void updateTimeSlot(WakeupTimeSlot timeSlot) {
        this.timeSlot = timeSlot;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }
}
