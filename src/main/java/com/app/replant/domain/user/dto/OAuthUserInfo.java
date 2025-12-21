package com.app.replant.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthUserInfo {
    private String providerId;
    private String email;
    private String nickname;
    private String profileImg;
}
