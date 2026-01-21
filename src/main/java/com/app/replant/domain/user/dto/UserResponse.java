package com.app.replant.domain.user.dto;

import com.app.replant.domain.mission.enums.PlaceType;
import com.app.replant.domain.mission.enums.WorryType;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.Gender;
import com.app.replant.domain.user.enums.UserRole;
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
    private UserRole role;

    // ============ 사용자 맞춤 정보 필드들 ============
    private WorryType worryType;
    private String region;
    private PlaceType preferredPlaceType;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .profileImg(user.getProfileImg())
                .createdAt(user.getCreatedAt())
                .role(user.getRole())
                // 사용자 맞춤 정보
                .worryType(user.getWorryType())
                .region(user.getRegion() != null ? user.getRegion().name() : null)
                .preferredPlaceType(user.getPreferredPlaceType())
                .build();
    }
}
