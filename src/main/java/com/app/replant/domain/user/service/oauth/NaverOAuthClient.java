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
public class NaverOAuthClient implements OAuthClient {

    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.NAVER;
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    NAVER_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new CustomException(ErrorCode.OAUTH_PROVIDER_ERROR);
            }

            Map<String, Object> responseData = (Map<String, Object>) body.get("response");

            String providerId = (String) responseData.get("id");
            String email = (String) responseData.get("email");
            String nickname = (String) responseData.get("nickname");
            String profileImg = (String) responseData.get("profile_image");

            return OAuthUserInfo.builder()
                    .providerId(providerId)
                    .email(email)
                    .nickname(nickname)
                    .profileImg(profileImg)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get Naver user info", e);
            throw new CustomException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
