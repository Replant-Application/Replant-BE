package com.app.replant.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "이메일 인증 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationDto {
    
    @Schema(description = "이메일 주소", example = "user@example.com", required = true)
    private String email;

    @Schema(description = "인증번호 (6자리)", example = "123456")
    private String code;
}

