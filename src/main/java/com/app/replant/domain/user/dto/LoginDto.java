package com.app.replant.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "로그인 요청 DTO")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDto {

    @Schema(description = "회원 ID (이메일)", example = "test@example.com")
    @JsonProperty("id")
    private String id;

    @Schema(description = "비밀번호", example = "password123")
    private String password;
}
