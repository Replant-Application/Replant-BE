package com.app.replant.domain.user.service;

import com.app.replant.controller.dto.OAuthLoginResponse;
import com.app.replant.controller.dto.TokenDto;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.dto.OAuthUserInfo;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.entity.UserOauth;
import com.app.replant.domain.user.enums.OAuthProvider;
import com.app.replant.domain.user.repository.UserOauthRepository;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.user.service.oauth.OAuthClient;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.app.replant.domain.user.security.UserDetail;
import com.app.replant.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuthService {

    private final List<OAuthClient> oAuthClients;
    private final UserRepository userRepository;
    private final UserOauthRepository userOauthRepository;
    private final ReantRepository reantRepository;
    private final TokenProvider tokenProvider;

    private Map<OAuthProvider, OAuthClient> oAuthClientMap;

    @jakarta.annotation.PostConstruct
    public void init() {
        oAuthClientMap = oAuthClients.stream()
                .collect(Collectors.toMap(OAuthClient::getProvider, Function.identity()));
    }

    @Transactional
    public OAuthLoginResponse login(OAuthProvider provider, String accessToken) {
        // 1. OAuth Provider에서 사용자 정보 가져오기
        OAuthClient oAuthClient = oAuthClientMap.get(provider);
        if (oAuthClient == null) {
            throw new CustomException(ErrorCode.INVALID_OAUTH_PROVIDER);
        }

        OAuthUserInfo userInfo = oAuthClient.getUserInfo(accessToken);

        // 2. 기존 OAuth 계정 확인
        UserOauth userOauth = userOauthRepository
                .findByProviderAndProviderId(provider, userInfo.getProviderId())
                .orElse(null);

        User user;
        boolean isNewUser;

        if (userOauth != null) {
            // 기존 회원 - 로그인
            user = userOauth.getUser();
            isNewUser = false;

            // Access Token 업데이트
            userOauth.updateTokens(accessToken, null);
        } else {
            // 신규 회원 - 회원가입
            // 이메일로 기존 User 확인
            user = userRepository.findByEmail(userInfo.getEmail()).orElse(null);

            if (user == null) {
                // 완전히 새로운 사용자
                user = User.builder()
                        .email(userInfo.getEmail())
                        .nickname(userInfo.getNickname())
                        .profileImg(userInfo.getProfileImg())
                        .build();
                userRepository.save(user);

                // Reant 생성
                Reant reant = Reant.builder()
                        .user(user)
                        .name(userInfo.getNickname() + "의 리앤트")
                        .level(1)
                        .exp(0)
                        .build();
                reantRepository.save(reant);
            }

            // OAuth 연동 정보 저장
            userOauth = UserOauth.builder()
                    .user(user)
                    .provider(provider)
                    .providerId(userInfo.getProviderId())
                    .accessToken(accessToken)
                    .build();
            userOauthRepository.save(userOauth);

            isNewUser = true;
        }

        // 3. JWT 토큰 생성
        UserDetail userDetail = new UserDetail(user);
        TokenDto tokenDto = tokenProvider.generateTokenDto(userDetail);

        // 4. 응답 생성
        return OAuthLoginResponse.of(tokenDto, user, isNewUser);
    }
}
