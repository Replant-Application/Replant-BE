package com.app.replant.global.infrastructure.service.auth;



import com.app.replant.domain.reant.dto.ReantResponse;
import com.app.replant.domain.reant.dto.ReantStatusResponse;
import com.app.replant.domain.reant.dto.ReantUpdateRequest;
import com.app.replant.domain.user.dto.CategorySpendDto;
import com.app.replant.domain.user.dto.ChangePasswordDto;
import com.app.replant.domain.user.dto.EmailVerificationDto;
import com.app.replant.domain.user.dto.JoinDto;
import com.app.replant.domain.user.dto.LoginDto;
import com.app.replant.domain.user.dto.LoginResponseDto;
import com.app.replant.domain.user.dto.MainDto;
import com.app.replant.domain.user.dto.OAuthLoginRequest;
import com.app.replant.domain.user.dto.OAuthLoginResponse;
import com.app.replant.domain.user.dto.ResetPasswordDto;
import com.app.replant.domain.user.dto.RestoreAccountRequest;
import com.app.replant.domain.user.dto.TokenDto;
import com.app.replant.domain.user.dto.TokenRequestDto;
import com.app.replant.domain.user.dto.UpdateEssentialCategoriesRequest;
import com.app.replant.domain.user.dto.UserDto;
import com.app.replant.domain.user.dto.UserResponse;
import com.app.replant.domain.user.dto.UserResponseDto;
import com.app.replant.domain.user.dto.UserUpdateRequest;
import com.app.replant.global.dto.AdminDiaryNotificationRequestDto;
import com.app.replant.global.dto.NotificationSendRequestDto;
import com.app.replant.global.dto.SseSendRequestDto;
import com.app.replant.global.dto.UploadedFileInfoDto;

import com.app.replant.domain.user.entity.User;

import java.util.Optional;

/**
 * 인증 서비스 인터페이스 (User 엔티티 기반)
 */
public interface AuthService {

    Optional<User> getUserByEmail(String email);

    LoginResponseDto login(String email, String password);

    LoginResponseDto join(JoinDto joinDto);

    Boolean checkId(String email);

    TokenDto refresh(TokenRequestDto tokenRequestDto);


    void logout(String email, String accessToken);

    void resetPassword(ResetPasswordDto resetPasswordDto);

    void changePassword(ChangePasswordDto changePasswordDto);

}