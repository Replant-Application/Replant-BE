package com.app.replant.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "업데이트 알림 전송 요청 DTO")
public class UpdateNotificationRequestDto {

    @Schema(description = "강제 업데이트 여부", example = "true", required = true)
    private boolean isRequired;

    @NotBlank(message = "메시지는 필수입니다")
    @Schema(description = "업데이트 메시지", example = "더 나은 서비스 이용을 위해 업데이트가 필요합니다.", required = true)
    private String message;

    @Schema(description = "스토어 URL", example = "https://play.google.com/store/apps/details?id=com.anonymous.replantmobileapp")
    private String storeUrl;
}
