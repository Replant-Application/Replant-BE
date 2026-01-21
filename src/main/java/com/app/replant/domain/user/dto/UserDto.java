package com.app.replant.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 DTO (User 엔티티 기반)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String email;
    private String nickname;
    private String phone;
    private String birthDate;
    private String gender;
    private String profileImg;
    private String role;
}
