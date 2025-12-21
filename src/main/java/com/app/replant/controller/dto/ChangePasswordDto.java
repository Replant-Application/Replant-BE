package com.app.replant.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "비밀번호 변경 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordDto {
    
    @Schema(description = "회원 ID (이메일)", example = "test@example.com")
    @JsonProperty("id")
    private String memberId;

    @Schema(description = "현재 비밀번호", example = "oldPassword123")
    @JsonProperty("oldPassword")
    private String oldPassword;

    @Schema(description = "새 비밀번호", example = "newPassword123")
    @JsonProperty("newPassword")
    private String newPassword;

    public String getCurrentPassword() {
        return this.oldPassword;
    }
}

