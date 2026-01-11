package com.app.replant.domain.user.service;

import com.app.replant.controller.dto.UserUpdateRequest;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.MetropolitanArea;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
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
}
