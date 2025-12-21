package com.app.replant.domain.user.service.oauth;

import com.app.replant.domain.user.dto.OAuthUserInfo;
import com.app.replant.domain.user.enums.OAuthProvider;

public interface OAuthClient {
    OAuthProvider getProvider();
    OAuthUserInfo getUserInfo(String accessToken);
}
