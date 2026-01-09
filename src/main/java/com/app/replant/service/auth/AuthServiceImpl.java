package com.app.replant.service.auth;


import com.app.replant.controller.dto.*;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.UserRole;
import com.app.replant.domain.user.enums.UserStatus;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.user.security.UserDetail;
import com.app.replant.exception.*;
import com.app.replant.jwt.*;
import com.app.replant.service.mailService.MailService;
import com.app.replant.service.token.RefreshTokenService;
import com.app.replant.service.token.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final ReantRepository reantRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public LoginResponseDto join(JoinDto joinDto) {
        try {
            // 1. 필수 요소 누락 확인
            if (joinDto.getId().isEmpty() || joinDto.getPassword().isEmpty() ||
                    joinDto.getName().isEmpty() || joinDto.getPhone().isEmpty()) {
                throw new CustomException(ErrorCode.REQUIRED_MISSING);
            }

            // 2. 형식 검증 (이메일, 비밀번호 등)
            if (!joinDto.getId().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                throw new CustomException(ErrorCode.INVALID_FORMAT);
            }
            if (joinDto.getPassword().length() < 8) {
                throw new CustomException(ErrorCode.INVALID_FORMAT);
            }

            // 3. 중복 회원 확인
            if (userRepository.existsByEmail(joinDto.getId())) {
                throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
            }

            // 4. 비밀번호 암호화 및 회원 저장
            String encodedPassword = passwordEncoder.encode(joinDto.getPassword());

            User user = User.builder()
                    .email(joinDto.getId())
                    .nickname(joinDto.getName())
                    .password(encodedPassword)
                    .phone(joinDto.getPhone())
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            User savedUser = userRepository.save(user);

            // 회원가입 시 기본 Reant(펫) 생성
            Reant reant = Reant.builder()
                    .user(savedUser)
                    .name("리앤트")
                    .build();
            reantRepository.save(reant);

            log.info("회원가입 완료: {}", joinDto.getId());

            // 5. 자동 로그인
            return login(joinDto.getId(), joinDto.getPassword());

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("회원가입 중 서버 오류 발생", e);
            throw new CustomException(ErrorCode.SIGNIN_FAIL);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkId(String email) {
        // 1. 비어있을 시
        if (email.isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        // 2. 형식 검증 (이메일)
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new CustomException(ErrorCode.INVALID_FORMAT);
        }
        // 3. 이미 존재하는 이메일인 경우 예외 발생
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        // 사용 가능한 이메일
        return true;
    }


    @Override
    @Transactional
    public LoginResponseDto login(String email, String password) {
        try {
            // 1. 사용자 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("로그인 실패 - 사용자 없음: {}", email);
                        return new CustomException(ErrorCode.USER_NOT_FOUND);
                    });

            // 2. 비밀번호 검증
            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.error("로그인 실패 - 비밀번호 불일치: {}", email);
                throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
            }

            // 3. 계정 상태 확인
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
            }

            // 4. UserDetail 생성 및 JWT 토큰 발급
            UserDetail userDetail = new UserDetail(user);
            TokenDto tokenDto = tokenProvider.generateTokenDto(userDetail);

            // 5. RefreshToken 저장 (Redis 또는 인메모리)
            refreshTokenService.saveRefreshToken(email, tokenDto.getRefreshToken());

            // 6. 마지막 로그인 시간 업데이트
            user.updateLastLoginAt();
            userRepository.save(user);

            log.info("로그인 성공: {}", email);

            // 7. LoginResponseDto 생성
            TokenRequestDto tokens = TokenRequestDto.builder()
                    .accessToken(tokenDto.getAccessToken())
                    .refreshToken(tokenDto.getRefreshToken())
                    .build();

            return LoginResponseDto.builder()
                    .name(userDetail.getNickname())
                    .tokens(tokens)
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("로그인 중 예외 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SIGNIN_FAIL);
        }
    }





    @Override
    @Transactional
    public TokenDto refresh(TokenRequestDto tokenRequestDto) {
        // 1. Refresh Token null/empty 체크
        if (tokenRequestDto.getRefreshToken() == null || tokenRequestDto.getRefreshToken().isEmpty()) {
            throw new CustomException(ErrorCode.NO_TOKEN);
        }

        // 2. Refresh Token 검증 (만료, 서명 등)
        tokenProvider.validateToken(tokenRequestDto.getRefreshToken());

        // 3. Access Token에서 User 이메일 가져오기
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());
        String email = authentication.getName();

        // 4. Redis에서 저장된 Refresh Token 조회
        String savedRefreshToken = refreshTokenService.getRefreshToken(email)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_REVOKED));

        // 5. Refresh Token 일치하는지 검사
        if (!savedRefreshToken.equals(tokenRequestDto.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 6. 새로운 토큰 생성
        UserDetail userDetail = (UserDetail) authentication.getPrincipal();
        TokenDto tokenDto = tokenProvider.generateTokenDto(userDetail);

        // 7. Redis에 새로운 Refresh Token 저장
        refreshTokenService.saveRefreshToken(email, tokenDto.getRefreshToken());

        return tokenDto;
    }

    @Override
    public User findUserByNicknameAndPhone(MemberSearchIdDto memberSearchIdDto) {
        if (memberSearchIdDto.getName().isEmpty() || memberSearchIdDto.getPhone().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_FORMAT);
        }
        User user = userRepository.findByNicknameAndPhone(memberSearchIdDto.getName(), memberSearchIdDto.getPhone())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        return user;
    }
    
    @Override
    public void logout(String email, String accessToken) {
        // 1. AccessToken의 남은 유효기간 계산
        long remainingSeconds = tokenProvider.getRemainingExpirationTime(accessToken);
        
        // 2. AccessToken을 블랙리스트에 등록 (남은 유효기간만큼)
        if (remainingSeconds > 0) {
            tokenBlacklistService.addToBlacklist(accessToken, remainingSeconds);
        }
        
        // 3. Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(email);
        
        log.info("로그아웃 완료: {} (AccessToken 블랙리스트 등록, 남은 유효기간: {}초)", email, remainingSeconds);
    }
    
    /**
     * 비밀번호 재설정 (임시 비밀번호 발급)
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        // 1. 필수 요소 검증 (이메일만 필수)
        if (resetPasswordDto.getMemberId() == null || resetPasswordDto.getMemberId().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }

        // 2. 회원 조회 (이메일로 확인)
        User user = userRepository.findByEmail(resetPasswordDto.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 계정 상태 확인
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        // 4. 임시 비밀번호 생성 및 이메일 발송
        String tempPassword = mailService.sendTemporaryPassword(
                resetPasswordDto.getMemberId(),
                user.getNickname()
        );
        log.info("임시 비밀번호 발급 및 이메일 발송 완료: {}", resetPasswordDto.getMemberId());

        // 6. DB에 임시 비밀번호 저장 (암호화)
        String encodedPassword = passwordEncoder.encode(tempPassword);
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        log.info("비밀번호 재설정 완료: {}", resetPasswordDto.getMemberId());
    }
    
    /**
     * 비밀번호 변경 (기존 비밀번호 확인 후 변경)
     */
    @Override
    @Transactional
    public void changePassword(ChangePasswordDto changePasswordDto) {
        // 1. 필수 요소 검증
        if (changePasswordDto.getMemberId() == null || changePasswordDto.getMemberId().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        if (changePasswordDto.getOldPassword() == null || changePasswordDto.getOldPassword().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        if (changePasswordDto.getNewPassword() == null || changePasswordDto.getNewPassword().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }

        // 2. 회원 조회
        User user = userRepository.findByEmail(changePasswordDto.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 계정 상태 확인
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        // 4. 기존 비밀번호 확인
        if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), user.getPassword())) {
            log.error("비밀번호 변경 실패: 기존 비밀번호 불일치 - Email: {}", changePasswordDto.getMemberId());
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 5. 새 비밀번호와 기존 비밀번호가 같은지 확인
        if (changePasswordDto.getOldPassword().equals(changePasswordDto.getNewPassword())) {
            log.error("비밀번호 변경 실패: 새 비밀번호가 기존 비밀번호와 동일 - Email: {}", changePasswordDto.getMemberId());
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }

        // 6. 새 비밀번호 암호화 및 저장
        String encodedNewPassword = passwordEncoder.encode(changePasswordDto.getNewPassword());
        user.updatePassword(encodedNewPassword);
        userRepository.save(user);

        log.info("비밀번호 변경 완료: {}", changePasswordDto.getMemberId());
    }

}