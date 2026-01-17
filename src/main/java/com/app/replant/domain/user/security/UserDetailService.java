package com.app.replant.domain.user.security;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.UserStatus;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService 구현
 * 이메일 기반 사용자 인증
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetail loadUserByUsername(String email) throws UsernameNotFoundException {
        // JWT 인증에는 Reant가 필요 없으므로 User만 조회 (불필요한 JOIN 방지)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 계정 상태 확인
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return new UserDetail(user);
    }
}
