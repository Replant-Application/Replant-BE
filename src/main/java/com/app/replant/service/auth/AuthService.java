package com.app.replant.service.auth;


import com.app.replant.controller.dto.*;
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

    TokenDto reissue(TokenRequestDto tokenRequestDto);

    User findUserByNicknameAndPhone(MemberSearchIdDto memberSearchIdDto);

    void logout(String email);

    void resetPassword(ResetPasswordDto resetPasswordDto);

    void changePassword(ChangePasswordDto changePasswordDto);

}