package com.app.replant.domain.user.service;

import com.app.replant.domain.user.dto.OAuthLoginResponse;
import com.app.replant.domain.user.dto.TokenDto;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.dto.OAuthUserInfo;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.entity.UserOauth;
import com.app.replant.domain.user.enums.OAuthProvider;
import com.app.replant.domain.user.repository.UserOauthRepository;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.user.service.oauth.OAuthClient;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import com.app.replant.domain.user.security.UserDetail;
import com.app.replant.global.security.jwt.TokenProvider;
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
    private final com.app.replant.domain.notification.service.NotificationService notificationService;

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

        // 2. 기존 OAuth 계정 확인 (User 정보와 함께 JOIN FETCH로 조회)
        UserOauth userOauth = userOauthRepository
                .findByProviderAndProviderIdWithUser(provider, userInfo.getProviderId())
                .orElse(null);

        User user;
        boolean isNewUser;

        if (userOauth != null) {
            // 기존 회원 - 로그인
            user = userOauth.getUser();
            isNewUser = false;

            // Access Token 업데이트
            userOauth.updateTokens(accessToken, null);

            // 마지막 로그인 시간 업데이트 (로그인 스트릭, lastLoginDate 포함)
            user.updateLastLoginAt();
            userRepository.save(user);
        } else {
            // 신규 회원 - 회원가입
            // 이메일로 기존 User 확인 - N+1 문제 방지를 위해 reant를 함께 로드
            user = userRepository.findByEmailWithReant(userInfo.getEmail()).orElse(null);

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

            // 신규 회원 첫 로그인 시간 기록
            user.updateLastLoginAt();
            userRepository.save(user);

            // 신규 유저에게 투두리스트 생성 알림 발송
            try {
                notificationService.createAndPushNotification(
                        user,
                        com.app.replant.domain.notification.enums.NotificationType.SYSTEM,
                        "환영합니다!",
                        "투두리스트를 생성해주세요!");
                log.info("신규 유저 투두리스트 생성 알림 발송 완료: userId={}", user.getId());
            } catch (Exception e) {
                log.error("신규 유저 알림 발송 실패: userId={}", user.getId(), e);
                // 알림 실패는 로그인 흐름을 방해하지 않음
            }
        }

        // 3. JWT 토큰 생성
        UserDetail userDetail = new UserDetail(user);
        TokenDto tokenDto = tokenProvider.generateTokenDto(userDetail);

        // 4. 응답 생성
        return OAuthLoginResponse.of(tokenDto, user, isNewUser);
    }
}
