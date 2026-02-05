package com.app.replant.domain.user.security;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.UserStatus;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
        // N+1 문제 방지를 위해 reant를 함께 로드
        User user = userRepository.findByEmailWithReant(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 계정 상태 확인
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return new UserDetail(user);
    }

    /**
     * 사용자 정보가 변경되었을 때 캐시를 무효화
     * (예: 사용자 정보 수정, 비밀번호 변경 등)
     */
    @CacheEvict(value = "userDetails", key = "#email")
    public void evictUserCache(String email) {
        log.debug("사용자 캐시 무효화: {}", email);
    }
}
