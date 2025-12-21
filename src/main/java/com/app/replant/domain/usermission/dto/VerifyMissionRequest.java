package com.app.replant.domain.usermission.dto;

import com.app.replant.domain.mission.enums.VerificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class VerifyMissionRequest {

    @NotNull(message = "인증 타입은 필수입니다.")
    private VerificationType type;

    // GPS 인증용
    private BigDecimal latitude;
    private BigDecimal longitude;

    // TIME 인증용
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
