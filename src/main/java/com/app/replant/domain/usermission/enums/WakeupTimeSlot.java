package com.app.replant.domain.usermission.enums;

import lombok.Getter;

/**
 * 기상 미션 시간대
 */
@Getter
public enum WakeupTimeSlot {
    SLOT_6_8("06:00-08:00", 6, 8),
    SLOT_8_10("08:00-10:00", 8, 10),
    SLOT_10_12("10:00-12:00", 10, 12);

    private final String displayName;
    private final int startHour;
    private final int endHour;

    WakeupTimeSlot(String displayName, int startHour, int endHour) {
        this.displayName = displayName;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public boolean isWithinTimeSlot(int hour) {
        return hour >= startHour && hour < endHour;
    }
}
