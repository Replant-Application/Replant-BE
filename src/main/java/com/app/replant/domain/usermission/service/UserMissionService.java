package com.app.replant.domain.usermission.service;

import com.app.replant.domain.badge.entity.UserBadge;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.dto.*;
import com.app.replant.domain.usermission.entity.MissionVerification;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.MissionVerificationRepository;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserMissionService {

    private final UserMissionRepository userMissionRepository;
    private final MissionVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ReantRepository reantRepository;

    public Page<UserMissionResponse> getUserMissions(Long userId, UserMissionStatus status, String missionType,
            Pageable pageable) {
        return userMissionRepository.findByUserIdWithFilters(userId, status, missionType, pageable)
                .map(UserMissionResponse::from);
    }

    public UserMissionResponse getUserMission(Long userMissionId, Long userId) {
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));
        return UserMissionResponse.from(userMission);
    }

    @Transactional
    public UserMissionResponse addCustomMission(Long userId, AddCustomMissionRequest request) {
        User user = findUserById(userId);
        Mission mission = missionRepository.findCustomMissionById(request.getCustomMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_MISSION_NOT_FOUND));

        // 커스텀 미션이 아닌 경우 에러
        if (!mission.isCustomMission()) {
            throw new CustomException(ErrorCode.CUSTOM_MISSION_NOT_FOUND);
        }

        // 비공개 미션이고 생성자가 아닌 경우
        if (!Boolean.TRUE.equals(mission.getIsPublic()) && !mission.isCreator(userId)) {
            throw new CustomException(ErrorCode.CUSTOM_MISSION_NOT_PUBLIC);
        }

        LocalDateTime now = LocalDateTime.now();
        // durationDays 또는 deadlineDays 사용, 없으면 기본 3일
        int days = mission.getDurationDays() != null ? mission.getDurationDays()
                : (mission.getDeadlineDays() != null ? mission.getDeadlineDays() : 3);
        LocalDateTime dueDate = now.plusDays(days);

        UserMission userMission = UserMission.builder()
                .user(user)
                .mission(mission)
                .missionType(MissionType.CUSTOM)
                .assignedAt(now)
                .dueDate(dueDate)
                .status(UserMissionStatus.ASSIGNED)
                .build();

        UserMission saved = userMissionRepository.save(userMission);
        return UserMissionResponse.from(saved);
    }

    @Transactional
    public UserMissionResponse addMission(Long userId, AddMissionRequest request) {
        User user = findUserById(userId);
        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        // durationDays 사용, 없으면 기본 3일
        int durationDays = mission.getDurationDays() != null ? mission.getDurationDays() : 3;
        LocalDateTime dueDate = now.plusDays(durationDays);

        UserMission userMission = UserMission.builder()
                .user(user)
                .mission(mission)
                .assignedAt(now)
                .dueDate(dueDate)
                .status(UserMissionStatus.ASSIGNED)
                .build();

        UserMission saved = userMissionRepository.save(userMission);
        return UserMissionResponse.from(saved);
    }

    @Transactional
    public void completeMissionVerification(UserMission userMission) {
        if (userMission.getStatus() == UserMissionStatus.COMPLETED) {
            return;
        }

        // 미션 완료 처리
        userMission.updateStatus(UserMissionStatus.COMPLETED);

        // 보상 지급 (커스텀 미션은 경험치 지급 없음)
        User user = userMission.getUser();
        int expReward = getExpReward(userMission);

        if (expReward > 0) {  // 커스텀 미션은 0 반환
            reantRepository.findByUserId(user.getId())
                    .ifPresent(reant -> reant.addExp(expReward));
        }

        // 뱃지 발급
        createBadge(userMission);

        log.info("Social Verification Completed: userMissionId={}, userId={}", userMission.getId(), user.getId());
    }

    @Transactional
    public VerifyMissionResponse verifyMission(Long userMissionId, Long userId, VerifyMissionRequest request) {
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        if (userMission.getStatus() != UserMissionStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_VERIFIED);
        }

        VerificationType requiredType = getVerificationType(userMission);
        if (requiredType != request.getType()) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }

        MissionVerification verification = null;

        if (request.getType() == VerificationType.GPS) {
            verification = verifyGPS(userMission, request);
        } else if (request.getType() == VerificationType.TIME) {
            verification = verifyTime(userMission, request);
        } else {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }

        // 미션 완료 처리
        userMission.updateStatus(UserMissionStatus.COMPLETED);

        // 보상 지급 (커스텀 미션은 경험치 지급 없음)
        User user = userMission.getUser();
        int expReward = getExpReward(userMission);

        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REANT_NOT_FOUND));
        if (expReward > 0) {  // 커스텀 미션은 0 반환
            reant.addExp(expReward);
        }

        // 뱃지 발급
        UserBadge badge = createBadge(userMission);

        // 유사 유저 추천 생성 (RecommendationService 삭제로 인해 임시 비활성화)
        // TODO: 추천 시스템 재구현 시 활성화
        log.debug("유사 유저 추천 기능 비활성화됨 - userMissionId={}", userMission.getId());
        return buildVerifyResponse(userMission, verification, expReward, badge);
    }

    /**
     * 미션 수행 이력 조회
     */
    public Page<UserMissionResponse> getMissionHistory(Long userId, Pageable pageable) {
        return userMissionRepository.findMissionHistoryByUserId(userId, pageable)
                .map(UserMissionResponse::from);
    }

    private MissionVerification verifyGPS(UserMission userMission, VerifyMissionRequest request) {
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new CustomException(ErrorCode.INVALID_GPS_DATA);
        }

        BigDecimal targetLat = getGpsLatitude(userMission);
        BigDecimal targetLon = getGpsLongitude(userMission);
        Integer radiusMeters = getGpsRadius(userMission);

        if (targetLat == null || targetLon == null) {
            throw new CustomException(ErrorCode.GPS_NOT_REQUIRED);
        }

        // Haversine 공식으로 거리 계산
        int distance = calculateDistance(
                request.getLatitude(),
                request.getLongitude(),
                targetLat,
                targetLon);

        if (distance > radiusMeters) {
            throw new CustomException(ErrorCode.GPS_OUT_OF_RANGE,
                    String.format("목표 위치에서 %dm 떨어져 있습니다. (허용 범위: %dm)", distance, radiusMeters));
        }

        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .gpsLatitude(request.getLatitude())
                .gpsLongitude(request.getLongitude())
                .gpsDistanceMeters(distance)
                .verifiedAt(LocalDateTime.now())
                .build();

        return verificationRepository.save(verification);
    }

    private MissionVerification verifyTime(UserMission userMission, VerifyMissionRequest request) {
        if (request.getStartedAt() == null || request.getEndedAt() == null) {
            throw new CustomException(ErrorCode.INVALID_TIME_DATA);
        }

        Integer requiredMinutes = getRequiredMinutes(userMission);
        if (requiredMinutes == null) {
            throw new CustomException(ErrorCode.TIME_NOT_REQUIRED);
        }

        Duration duration = Duration.between(request.getStartedAt(), request.getEndedAt());
        int actualMinutes = (int) duration.toMinutes();

        if (actualMinutes < requiredMinutes) {
            throw new CustomException(ErrorCode.TIME_NOT_ENOUGH,
                    String.format("실제 시간: %d분, 필요 시간: %d분", actualMinutes, requiredMinutes));
        }

        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .timeStartedAt(request.getStartedAt())
                .timeEndedAt(request.getEndedAt())
                .timeActualMinutes(actualMinutes)
                .verifiedAt(LocalDateTime.now())
                .build();

        return verificationRepository.save(verification);
    }

    private UserBadge createBadge(UserMission userMission) {
        LocalDateTime now = LocalDateTime.now();
        Integer badgeDurationDays = getBadgeDurationDays(userMission);
        LocalDateTime expiresAt = now.plusDays(badgeDurationDays);

        UserBadge badge = UserBadge.builder()
                .user(userMission.getUser())
                .mission(userMission.getMission())
                .userMission(userMission)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();

        return userBadgeRepository.save(badge);
    }

    private VerifyMissionResponse buildVerifyResponse(UserMission userMission, MissionVerification verification,
            int expReward, UserBadge badge) {
        VerifyMissionResponse.VerificationInfo verificationInfo = null;

        if (verification.getGpsLatitude() != null) {
            verificationInfo = VerifyMissionResponse.VerificationInfo.builder()
                    .gpsLatitude(verification.getGpsLatitude())
                    .gpsLongitude(verification.getGpsLongitude())
                    .gpsDistanceMeters(verification.getGpsDistanceMeters())
                    .verifiedAt(verification.getVerifiedAt())
                    .build();
        } else if (verification.getTimeStartedAt() != null) {
            verificationInfo = VerifyMissionResponse.VerificationInfo.builder()
                    .timeStartedAt(verification.getTimeStartedAt())
                    .timeEndedAt(verification.getTimeEndedAt())
                    .timeActualMinutes(verification.getTimeActualMinutes())
                    .verifiedAt(verification.getVerifiedAt())
                    .build();
        }

        VerifyMissionResponse.BadgeInfo badgeInfo = VerifyMissionResponse.BadgeInfo.builder()
                .id(badge.getId())
                .expiresAt(badge.getExpiresAt())
                .build();

        VerifyMissionResponse.RewardInfo rewardInfo = VerifyMissionResponse.RewardInfo.builder()
                .expEarned(expReward)
                .badge(badgeInfo)
                .build();

        return VerifyMissionResponse.builder()
                .userMissionId(userMission.getId())
                .status(userMission.getStatus())
                .verification(verificationInfo)
                .rewards(rewardInfo)
                .build();
    }

    // Haversine 공식 - 두 GPS 좌표 간 거리 계산 (미터)
    private int calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        final int EARTH_RADIUS = 6371000; // 지구 반지름 (미터)

        double dLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double dLon = Math.toRadians(lon2.subtract(lon1).doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1.doubleValue())) *
                        Math.cos(Math.toRadians(lat2.doubleValue())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) (EARTH_RADIUS * c);
    }

    private VerificationType getVerificationType(UserMission userMission) {
        Mission mission = userMission.getMission();
        if (mission != null) {
            return mission.getVerificationType();
        }
        throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
    }

    private BigDecimal getGpsLatitude(UserMission userMission) {
        // GPS 인증 기능 미사용
        return null;
    }

    private BigDecimal getGpsLongitude(UserMission userMission) {
        // GPS 인증 기능 미사용
        return null;
    }

    private Integer getGpsRadius(UserMission userMission) {
        // GPS 인증 기능 미사용
        return 100;
    }

    private Integer getRequiredMinutes(UserMission userMission) {
        Mission mission = userMission.getMission();
        return mission != null ? mission.getRequiredMinutes() : null;
    }

    /**
     * 미션 경험치 보상 계산
     * 커스텀 미션은 경험치를 지급하지 않으므로 항상 0을 반환
     */
    private int getExpReward(UserMission userMission) {
        Mission mission = userMission.getMission();
        if (mission == null) {
            return 10;  // 기본값 (공식 미션 가정)
        }
        // 커스텀 미션은 항상 0 반환
        if (mission.isCustomMission()) {
            return 0;
        }
        return mission.getExpReward();
    }

    private Integer getBadgeDurationDays(UserMission userMission) {
        Mission mission = userMission.getMission();
        return mission != null ? mission.calculateBadgeDuration() : 3;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
