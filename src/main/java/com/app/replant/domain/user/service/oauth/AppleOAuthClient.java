package com.app.replant.domain.user.service.oauth;

import com.app.replant.domain.user.dto.OAuthUserInfo;
import com.app.replant.domain.user.enums.OAuthProvider;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleOAuthClient implements OAuthClient {

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.APPLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(String identityToken) {
        try {
            // Apple의 경우 identity token을 직접 파싱
            // identity token은 JWT 형식이며, 공개 키 검증은 별도로 필요
            String[] chunks = identityToken.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();

            String payload = new String(decoder.decode(chunks[1]));

            // JWT payload 파싱을 위해 간단한 방법 사용
            // 실제 프로덕션에서는 Apple 공개키로 서명 검증 필요
            int unsignedStartIndex = identityToken.lastIndexOf('.') + 1;
            String withoutSignature = identityToken.substring(0, unsignedStartIndex);

            Claims claims = Jwts.parser()
                    .unsecured()
                    .build()
                    .parseUnsecuredClaims(withoutSignature)
                    .getPayload();

            String providerId = claims.getSubject();
            String email = claims.get("email", String.class);

            // Apple은 닉네임과 프로필 이미지를 제공하지 않음
            // 최초 로그인 시에만 user 정보를 별도로 제공받을 수 있음
            String nickname = email != null ? email.split("@")[0] : "User";

            return OAuthUserInfo.builder()
                    .providerId(providerId)
                    .email(email)
                    .nickname(nickname)
                    .profileImg(null)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get Apple user info", e);
            throw new CustomException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
