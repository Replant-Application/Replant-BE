package com.app.replant.domain.meallog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MealType {
    BREAKFAST("아침", "08:00"),
    LUNCH("점심", "12:00"),
    DINNER("저녁", "18:00");

    private final String displayName;
    private final String defaultTime;

    public static MealType fromDisplayName(String displayName) {
        for (MealType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown meal type: " + displayName);
    }
}
