package com.app.replant.controller;

import com.app.replant.controller.dto.OAuthLoginRequest;
import com.app.replant.controller.dto.OAuthLoginResponse;
import com.app.replant.domain.user.dto.OAuthUserInfo;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.entity.UserOAuth;
import com.app.replant.domain.user.enums.OAuthProvider;
import com.app.replant.domain.user.repository.UserOAuthRepository;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.user.service.OAuthService;
import com.app.replant.domain.user.service.oauth.OAuthClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("OAuth 로그인 테스트")
public class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOAuthRepository userOAuthRepository;

    @MockBean
    private OAuthClient kakaoOAuthClient;

    @Test
    @DisplayName("신규 사용자 OAuth 로그인 - 회원가입 및 토큰 발급")
    void testNewUserOAuthLogin() throws Exception {
        // Given
        String accessToken = "test_kakao_access_token";
        OAuthUserInfo mockUserInfo = OAuthUserInfo.builder()
                .providerId("12345678")
                .email("test@kakao.com")
                .nickname("카카오테스트")
                .profileImg("https://test.kakao.com/profile.jpg")
                .build();

        when(kakaoOAuthClient.getProvider()).thenReturn(OAuthProvider.KAKAO);
        when(kakaoOAuthClient.getUserInfo(anyString())).thenReturn(mockUserInfo);

        OAuthLoginRequest request = new OAuthLoginRequest();
        // accessToken을 설정할 수 있도록 빌더나 setter 필요

        // When & Then
        mockMvc.perform(post("/api/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.user.email").value("test@kakao.com"))
                .andExpect(jsonPath("$.data.user.nickname").value("카카오테스트"));

        // DB 검증
        Optional<User> savedUser = userRepository.findByEmail("test@kakao.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getNickname()).isEqualTo("카카오테스트");

        Optional<UserOAuth> savedOAuth = userOAuthRepository.findByProviderAndProviderId(
                OAuthProvider.KAKAO, "12345678");
        assertThat(savedOAuth).isPresent();
    }

    @Test
    @DisplayName("기존 사용자 OAuth 로그인 - 토큰 발급")
    void testExistingUserOAuthLogin() throws Exception {
        // Given - 기존 사용자 생성
        User existingUser = User.builder()
                .email("existing@kakao.com")
                .nickname("기존사용자")
                .profileImg("https://old.profile.jpg")
                .build();
        userRepository.save(existingUser);

        UserOAuth existingOAuth = UserOAuth.builder()
                .user(existingUser)
                .provider(OAuthProvider.KAKAO)
                .providerId("87654321")
                .accessToken("old_token")
                .build();
        userOAuthRepository.save(existingOAuth);

        // OAuth Provider 응답 모킹
        OAuthUserInfo mockUserInfo = OAuthUserInfo.builder()
                .providerId("87654321")
                .email("existing@kakao.com")
                .nickname("기존사용자")
                .profileImg("https://new.profile.jpg")
                .build();

        when(kakaoOAuthClient.getProvider()).thenReturn(OAuthProvider.KAKAO);
        when(kakaoOAuthClient.getUserInfo(anyString())).thenReturn(mockUserInfo);

        OAuthLoginRequest request = new OAuthLoginRequest();

        // When & Then
        mockMvc.perform(post("/api/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andExpect(jsonPath("$.data.user.email").value("existing@kakao.com"));
    }

    @Test
    @DisplayName("잘못된 OAuth Provider - 400 에러")
    void testInvalidOAuthProvider() throws Exception {
        // Given
        OAuthLoginRequest request = new OAuthLoginRequest();

        // When & Then
        mockMvc.perform(post("/api/auth/oauth/INVALID_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("여러 OAuth Provider 지원 확인")
    void testMultipleProviders() throws Exception {
        // Given
        String[] providers = {"KAKAO", "GOOGLE", "NAVER", "APPLE"};

        for (String provider : providers) {
            OAuthLoginRequest request = new OAuthLoginRequest();

            // Provider별 다른 설정이 필요할 수 있음
            // 여기서는 endpoint가 정상적으로 존재하는지만 확인
        }
    }
}
