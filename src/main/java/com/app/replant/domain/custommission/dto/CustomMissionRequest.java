package com.app.replant.domain.custommission.dto;

import com.app.replant.domain.mission.enums.DifficultyLevel;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.enums.WorryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CustomMissionRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "설명은 필수입니다.")
    private String description;

    // 고민 종류 (선택)
    private WorryType worryType;

    // 미션 타입 (카테고리): DAILY_LIFE(일상), GROWTH(성장), EXERCISE(운동), STUDY(학습), HEALTH(건강), RELATIONSHIP(관계)
    private MissionType missionType;

    // 난이도: EASY(쉬움), MEDIUM(보통), HARD(어려움)
    private DifficultyLevel difficultyLevel;

    // 챌린지 기간 (일수) - 예: 7일 챌린지면 7, 30일 챌린지면 30
    @Min(value = 1, message = "챌린지 기간은 1일 이상이어야 합니다.")
    private Integer challengeDays;

    // 완료 기한 (일수) - 미션 할당 후 N일 이내 완료해야 함 (기본값: 3)
    @Min(value = 1, message = "완료 기한은 1일 이상이어야 합니다.")
    private Integer deadlineDays;

    @NotNull(message = "기간은 필수입니다.")
    @Min(value = 1, message = "기간은 1일 이상이어야 합니다.")
    private Integer durationDays;

    @NotNull(message = "공개 여부는 필수입니다.")
    private Boolean isPublic;

    @NotNull(message = "인증 타입은 필수입니다.")
    private VerificationType verificationType;

    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
    private Integer gpsRadiusMeters;
    private Integer requiredMinutes;

    // 경험치 보상 (선택 - 난이도에 따라 자동 계산 가능)
    @Min(value = 0, message = "경험치는 0 이상이어야 합니다.")
    private Integer expReward;

    @NotNull(message = "뱃지 유효 기간은 필수입니다.")
    @Min(value = 1, message = "뱃지 유효 기간은 1일 이상이어야 합니다.")
    private Integer badgeDurationDays;
}
