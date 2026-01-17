package com.app.replant.domain.user.service;

import com.app.replant.controller.dto.UserUpdateRequest;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.MetropolitanArea;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // Soft Delete된 사용자 체크
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        
        return user;
    }

    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // Soft Delete된 사용자 체크
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        
        return user;
    }

    @Transactional
    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = findById(userId);

        // region 문자열을 MetropolitanArea enum으로 변환
        MetropolitanArea region = null;
        if (request.getRegion() != null && !request.getRegion().isEmpty()) {
            try {
                region = MetropolitanArea.valueOf(request.getRegion());
            } catch (IllegalArgumentException e) {
                // 유효하지 않은 region 값은 무시
            }
        }

        user.updateProfile(
                request.getNickname(),
                request.getBirthDate(),
                request.getGender(),
                request.getProfileImg(),
                // 사용자 맞춤 정보
                request.getWorryType(),
                region,
                request.getPreferredPlaceType()
        );
        return user;
    }

    /**
     * 회원 탈퇴 (Soft Delete)
     * - delFlag를 true로 설정
     * - status를 DELETED로 변경
     * - 개인정보 마스킹
     * - FCM 토큰 제거
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = findById(userId);
        
        // 이미 탈퇴한 사용자인지 확인
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }
        
        // Soft Delete 처리
        user.softDelete();
        userRepository.save(user);
        
        log.info("회원 탈퇴 처리 완료 (Soft Delete) - userId: {}, email: {}", userId, user.getEmail());
    }

    /**
     * 계정 복구 (30일 이내에만 가능) - 인증 없이 이메일 + 비밀번호로 복구
     * - delFlag를 false로 설정
     * - status를 ACTIVE로 변경
     * - deletedAt을 null로 초기화
     * 
     * @param email 복구할 사용자 이메일
     * @param password 비밀번호 (검증용)
     * @throws CustomException 탈퇴한 계정이 아니거나, 30일이 지났거나, 비밀번호가 틀린 경우
     */
    @Transactional
    public void restoreUserByEmail(String email, String password) {
        // Soft Delete된 사용자도 조회 가능하도록 별도 메서드 사용
        User user = userRepository.findByEmailIncludingDeleted(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // 탈퇴한 계정인지 확인
        if (!user.isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_DELETED);
        }
        
        // 비밀번호 검증
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 30일 이내인지 확인
        if (user.getDeletedAt() == null) {
            throw new CustomException(ErrorCode.USER_RESTORE_EXPIRED);
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        
        if (user.getDeletedAt().isBefore(thirtyDaysAgo)) {
            throw new CustomException(ErrorCode.USER_RESTORE_EXPIRED);
        }
        
        // 계정 복구 처리
        user.restore();
        userRepository.save(user);
        
        log.info("계정 복구 완료 (이메일 기반) - userId: {}, email: {}, deletedAt: {}", 
                user.getId(), user.getEmail(), user.getDeletedAt());
    }
}
