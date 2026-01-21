package com.app.replant.domain.graduation.service;

import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.enums.ReantStage;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.UserRole;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 졸업 시스템 서비스
 * - 리앤트가 ADULT 스테이지에 도달하고 일정 조건을 만족하면 졸업자로 전환
 * - 졸업자는 커뮤니티에서 멘토 역할을 수행
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class GraduationService {

    private final UserRepository userRepository;
    private final ReantRepository reantRepository;
    private final UserMissionRepository userMissionRepository;

    // 졸업 조건: 완료한 미션 수
    private static final int GRADUATION_REQUIRED_MISSIONS = 30;

    // 졸업 조건: 리앤트 최소 레벨
    private static final int GRADUATION_REQUIRED_LEVEL = 30;

    /**
     * 사용자가 졸업 조건을 만족하는지 확인
     */
    public GraduationCheckResult checkGraduationEligibility(Long userId) {
        User user = findUserById(userId);

        // 이미 졸업자인 경우
        if (user.getRole() == UserRole.GRADUATE) {
            return GraduationCheckResult.builder()
                    .eligible(false)
                    .alreadyGraduated(true)
                    .message("이미 졸업한 사용자입니다.")
                    .build();
        }

        // 리앤트 정보 확인
        Reant reant = reantRepository.findByUserId(userId)
                .orElse(null);

        if (reant == null) {
            return GraduationCheckResult.builder()
                    .eligible(false)
                    .message("리앤트 정보를 찾을 수 없습니다.")
                    .build();
        }

        // 리앤트 스테이지 확인
        boolean reantCondition = reant.getStage() == ReantStage.ADULT
                && reant.getLevel() >= GRADUATION_REQUIRED_LEVEL;

        // 완료한 미션 수 확인
        long completedMissions = userMissionRepository.countByUserIdAndStatus(
                userId, UserMissionStatus.COMPLETED);
        boolean missionCondition = completedMissions >= GRADUATION_REQUIRED_MISSIONS;

        boolean eligible = reantCondition && missionCondition;

        String message;
        if (eligible) {
            message = "축하합니다! 졸업 조건을 모두 만족했습니다.";
        } else {
            StringBuilder sb = new StringBuilder("졸업 조건을 충족하지 못했습니다. ");
            if (!reantCondition) {
                sb.append(String.format("리앤트 레벨 %d/%d 필요. ",
                        reant.getLevel(), GRADUATION_REQUIRED_LEVEL));
            }
            if (!missionCondition) {
                sb.append(String.format("완료 미션 %d/%d개 필요.",
                        completedMissions, GRADUATION_REQUIRED_MISSIONS));
            }
            message = sb.toString();
        }

        return GraduationCheckResult.builder()
                .eligible(eligible)
                .alreadyGraduated(false)
                .reantLevel(reant.getLevel())
                .reantStage(reant.getStage().name())
                .completedMissions((int) completedMissions)
                .requiredLevel(GRADUATION_REQUIRED_LEVEL)
                .requiredMissions(GRADUATION_REQUIRED_MISSIONS)
                .message(message)
                .build();
    }

    /**
     * 졸업 처리 - 사용자를 GRADUATE 역할로 전환
     */
    @Transactional
    public GraduationResult graduate(Long userId) {
        GraduationCheckResult eligibility = checkGraduationEligibility(userId);

        if (!eligibility.isEligible()) {
            throw new CustomException(ErrorCode.GRADUATION_NOT_ELIGIBLE);
        }

        User user = findUserById(userId);
        user.updateRole(UserRole.GRADUATE);

        log.info("User {} graduated successfully", userId);

        return GraduationResult.builder()
                .success(true)
                .userId(userId)
                .newRole(UserRole.GRADUATE.name())
                .message("축하합니다! 리플랜트 졸업을 완료했습니다. " +
                        "이제 멘토로서 다른 사용자들을 도와줄 수 있습니다.")
                .build();
    }

    /**
     * 미션 완료 시 자동 졸업 체크
     * - 미션 완료 후 호출되어 졸업 조건 만족 시 자동 알림
     */
    public boolean checkAndNotifyGraduation(Long userId) {
        try {
            GraduationCheckResult result = checkGraduationEligibility(userId);
            if (result.isEligible() && !result.isAlreadyGraduated()) {
                // 졸업 가능 알림 전송 로직 추가 가능
                log.info("User {} is now eligible for graduation", userId);
                return true;
            }
        } catch (Exception e) {
            log.warn("Failed to check graduation eligibility for user {}", userId, e);
        }
        return false;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 졸업 조건 확인 결과
     */
    @lombok.Builder
    @lombok.Getter
    public static class GraduationCheckResult {
        private boolean eligible;
        private boolean alreadyGraduated;
        private Integer reantLevel;
        private String reantStage;
        private Integer completedMissions;
        private Integer requiredLevel;
        private Integer requiredMissions;
        private String message;
    }

    /**
     * 졸업 처리 결과
     */
    @lombok.Builder
    @lombok.Getter
    public static class GraduationResult {
        private boolean success;
        private Long userId;
        private String newRole;
        private String message;
    }
}
