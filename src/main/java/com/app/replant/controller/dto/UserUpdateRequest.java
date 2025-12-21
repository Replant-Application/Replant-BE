package com.app.replant.controller.dto;

import com.app.replant.domain.user.enums.Gender;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {
    private String nickname;
    private LocalDate birthDate;
    private Gender gender;
    private String profileImg;
}
