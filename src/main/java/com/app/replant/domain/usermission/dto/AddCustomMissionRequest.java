package com.app.replant.domain.usermission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddCustomMissionRequest {

    @NotNull(message = "커스텀 미션 ID는 필수입니다.")
    private Long customMissionId;
}
