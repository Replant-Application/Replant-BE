package com.app.replant.controller.dto;

import com.app.replant.domain.user.enums.Gender;
import com.app.replant.domain.user.enums.MetropolitanArea;
import com.app.replant.entity.Member;
import com.app.replant.entity.type.Authority;
import com.app.replant.entity.type.StatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "회원가입 요청 DTO")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinDto {

    @Schema(description = "회원 ID (이메일)", example = "test@example.com")
    private String id;

    @Schema(description = "비밀번호", example = "password123")
    private String password;

    @Schema(description = "회원 이름(닉네임)", example = "김두리")
    private String name;

    @Schema(description = "전화번호", example = "01012345678")
    private String phone;

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "지역 (광역자치단체)", example = "SEOUL")
    private MetropolitanArea region;

    @Schema(description = "출생연도", example = "1995")
    private Integer birthYear;

    /**
     * JoinDto를 Member 엔티티로 변환
     * @param encodedPassword 암호화된 비밀번호
     * @return Member 엔티티
     */
    public Member toEntity(String encodedPassword) {
        return Member.builder()
                .memberId(this.id)
                .password(encodedPassword)
                .memberName(this.name)
                .phone(this.phone)
                .status(StatusType.ABLE)
                .authority(Authority.USER)
                .build();
    }
}

