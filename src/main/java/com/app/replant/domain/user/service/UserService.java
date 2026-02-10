package com.app.replant.domain.user.service;

import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.diary.repository.DiaryRepository;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.domain.user.dto.SpontaneousMissionRequest;
import com.app.replant.domain.user.dto.SpontaneousMissionResponse;
import com.app.replant.domain.user.dto.UserStatsResponse;
import com.app.replant.domain.user.dto.UserUpdateRequest;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.MetropolitanArea;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMissionRepository userMissionRepository;
    private final PostRepository postRepository;
    private final DiaryRepository diaryRepository;
    private final UserBadgeRepository userBadgeRepository;

    /**
     * ID로 사용자 조회. N+1 방지를 위해 reant를 함께 로드(JOIN FETCH).
     */
    public User findById(Long userId) {
        User user = userRepository.findByIdWithReant(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    /**
     * 이메일로 사용자 조회. N+1 방지를 위해 reant를 함께 로드(JOIN FETCH).
     */
    public User findByEmail(String email) {
        User user = userRepository.findByEmailWithReant(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

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
                request.getPreferredPlaceType(),
                request.getPreferredMissionCategories()
        );
        return userRepository.save(user);
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

    /**
     * 돌발 미션 설정 조회
     * 설정이 없으면 404 (앱에서 신규 설정 모드로 전환)
     */
    public SpontaneousMissionResponse getSpontaneousMissionSetup(Long userId) {
        User user = findById(userId);
        if (!user.isSpontaneousMissionSetupCompleted() || user.getWakeTime() == null || user.getWakeTime().isEmpty()) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return SpontaneousMissionResponse.builder()
                .isSpontaneousMissionSetupCompleted(user.isSpontaneousMissionSetupCompleted())
                .wakeTime(user.getWakeTime())
                .build();
    }

    /**
     * 돌발 미션 설정 등록 (최초 설정, 기상 시간만)
     */
    @Transactional
    public SpontaneousMissionResponse setupSpontaneousMission(Long userId, SpontaneousMissionRequest request) {
        User user = findById(userId);
        user.setupSpontaneousMission(request.getWakeTime());
        User saved = userRepository.save(user);
        return SpontaneousMissionResponse.builder()
                .isSpontaneousMissionSetupCompleted(saved.isSpontaneousMissionSetupCompleted())
                .wakeTime(saved.getWakeTime())
                .build();
    }

    /**
     * 돌발 미션 설정 수정 (기상 시간만)
     */
    @Transactional
    public SpontaneousMissionResponse updateSpontaneousMissionSetup(Long userId, SpontaneousMissionRequest request) {
        User user = findById(userId);
        user.setupSpontaneousMission(request.getWakeTime());
        User saved = userRepository.save(user);
        return SpontaneousMissionResponse.builder()
                .isSpontaneousMissionSetupCompleted(saved.isSpontaneousMissionSetupCompleted())
                .wakeTime(saved.getWakeTime())
                .build();
    }

    /**
     * 마이 페이지 통계 정보 조회
     * 통계만 필요한 경우 대량 조회를 방지하기 위해 별도 API 제공
     * 모든 통계를 병렬로 조회하여 성능 최적화
     */
    public UserStatsResponse getUserStats(Long userId) {
        // 모든 통계를 병렬로 조회 (CompletableFuture 사용)
        CompletableFuture<Long> completedMissionsFuture = CompletableFuture.supplyAsync(() ->
                userMissionRepository.countByUserIdAndStatus(userId, UserMissionStatus.COMPLETED));
        
        CompletableFuture<Long> postsCountFuture = CompletableFuture.supplyAsync(() ->
                postRepository.countByUserId(userId));
        
        CompletableFuture<Long> approvedVerificationsFuture = CompletableFuture.supplyAsync(() ->
                postRepository.countApprovedVerificationsByUserId(userId));
        
        CompletableFuture<Long> diariesCountFuture = CompletableFuture.supplyAsync(() ->
                diaryRepository.countByUserId(userId));
        
        CompletableFuture<Long> badgesCountFuture = CompletableFuture.supplyAsync(() ->
                userBadgeRepository.countByUserId(userId));

        // 모든 통계 조회 완료 대기
        try {
            long completedMissionsCount = completedMissionsFuture.get();
            long postsCount = postsCountFuture.get();
            long approvedVerificationsCount = approvedVerificationsFuture.get();
            long diariesCount = diariesCountFuture.get();
            long badgesCount = badgesCountFuture.get();

            return UserStatsResponse.builder()
                    .completedMissionsCount(completedMissionsCount)
                    .postsCount(postsCount)
                    .approvedVerificationsCount(approvedVerificationsCount)
                    .diariesCount(diariesCount)
                    .badgesCount(badgesCount)
                    .build();
        } catch (Exception e) {
            log.error("통계 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
