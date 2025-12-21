package com.app.replant.domain.user.service.oauth;

import com.app.replant.domain.user.dto.OAuthUserInfo;
import com.app.replant.domain.user.enums.OAuthProvider;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new CustomException(ErrorCode.OAUTH_PROVIDER_ERROR);
            }

            String providerId = String.valueOf(body.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String email = (String) kakaoAccount.get("email");
            String nickname = (String) profile.get("nickname");
            String profileImg = (String) profile.get("profile_image_url");

            return OAuthUserInfo.builder()
                    .providerId(providerId)
                    .email(email)
                    .nickname(nickname)
                    .profileImg(profileImg)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get Kakao user info", e);
            throw new CustomException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
