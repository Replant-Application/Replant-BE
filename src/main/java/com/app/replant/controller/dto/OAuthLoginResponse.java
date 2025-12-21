package com.app.replant.controller.dto;

import com.app.replant.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthLoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
    private boolean isNewUser;

    public static OAuthLoginResponse of(TokenDto tokenDto, User user, boolean isNewUser) {
        return OAuthLoginResponse.builder()
                .accessToken(tokenDto.getAccessToken())
                .refreshToken(tokenDto.getRefreshToken())
                .user(UserResponse.from(user))
                .isNewUser(isNewUser)
                .build();
    }
}
