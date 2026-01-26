package com.app.replant.domain.meallog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MealLogStatus {
    ASSIGNED("할당됨"),      // 스케줄러가 할당함
    COMPLETED("완료"),       // 인증 완료
    FAILED("실패"),          // 시간 초과 등으로 실패
    SKIPPED("건너뜀");       // 사용자가 건너뜀

    private final String description;
}
