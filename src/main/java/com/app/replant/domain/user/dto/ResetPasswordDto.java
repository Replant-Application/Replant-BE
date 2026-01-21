package com.app.replant.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "비밀번호 재설정 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordDto {
    
    @Schema(description = "회원 ID (이메일)", example = "test@example.com")
    @JsonProperty("id")
    private String memberId;
    
    @Schema(description = "회원 이름", example = "석기시대")
    @JsonProperty("name")
    private String memberName;
}

