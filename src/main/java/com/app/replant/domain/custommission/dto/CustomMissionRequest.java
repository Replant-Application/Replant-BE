package com.app.replant.domain.custommission.dto;

import com.app.replant.domain.mission.enums.VerificationType;
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

    @NotNull(message = "경험치 보상은 필수입니다.")
    @Min(value = 0, message = "경험치는 0 이상이어야 합니다.")
    private Integer expReward;

    @NotNull(message = "뱃지 유효 기간은 필수입니다.")
    @Min(value = 1, message = "뱃지 유효 기간은 1일 이상이어야 합니다.")
    private Integer badgeDurationDays;
}
