package com.app.replant.controller.dto;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.Gender;
import com.app.replant.domain.user.enums.UserRole;
import com.app.replant.domain.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "회원 정보 응답 DTO (관리자용)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    @Schema(description = "회원 고유 번호", example = "1")
    private Long id;

    @Schema(description = "회원 이메일", example = "test@example.com")
    private String email;

    @Schema(description = "닉네임", example = "두리")
    private String nickname;

    @Schema(description = "전화번호", example = "01012345678")
    private String phone;

    @Schema(description = "생년월일", example = "1999-01-01")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "프로필 이미지 URL")
    private String profileImg;

    @Schema(description = "권한", example = "USER")
    private UserRole role;

    @Schema(description = "상태", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "마지막 로그인 시간")
    private LocalDateTime lastLoginAt;

    @Schema(description = "생성일")
    private LocalDateTime createdAt;

    @Schema(description = "완료한 미션 수")
    private Integer totalMissionsCompleted;

    @Schema(description = "획득한 총 경험치")
    private Integer totalExpGained;

    /**
     * Entity를 DTO로 변환 (비밀번호 제외)
     */
    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .profileImg(user.getProfileImg())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .totalMissionsCompleted(user.getTotalMissionsCompleted())
                .totalExpGained(user.getTotalExpGained())
                .build();
    }
}
