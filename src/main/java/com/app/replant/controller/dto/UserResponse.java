package com.app.replant.controller.dto;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.Gender;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private LocalDate birthDate;
    private Gender gender;
    private String profileImg;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .profileImg(user.getProfileImg())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
