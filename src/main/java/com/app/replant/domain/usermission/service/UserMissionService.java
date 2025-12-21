package com.app.replant.domain.usermission.service;

import com.app.replant.domain.badge.entity.UserBadge;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.custommission.repository.CustomMissionRepository;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.recommendation.service.RecommendationService;
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
    private final CustomMissionRepository customMissionRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ReantRepository reantRepository;
    private final RecommendationService recommendationService;

    public Page<UserMissionResponse> getUserMissions(Long userId, UserMissionStatus status, String missionType, Pageable pageable) {
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
        CustomMission customMission = customMissionRepository.findById(request.getCustomMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_MISSION_NOT_FOUND));

        if (!customMission.getIsPublic() && !customMission.isCreator(userId)) {
            throw new CustomException(ErrorCode.CUSTOM_MISSION_NOT_PUBLIC);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusDays(customMission.getDurationDays());

        UserMission userMission = UserMission.builder()
                .user(user)
                .customMission(customMission)
                .assignedAt(now)
                .dueDate(dueDate)
                .status(UserMissionStatus.ASSIGNED)
                .build();

        UserMission saved = userMissionRepository.save(userMission);
        return UserMissionResponse.from(saved);
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

        // 보상 지급
        User user = userMission.getUser();
        int expReward = getExpReward(userMission);

        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REANT_NOT_FOUND));
        reant.addExp(expReward);

        // 뱃지 발급
        UserBadge badge = createBadge(userMission);

        // 유사 유저 추천 생성
        try {
            recommendationService.generateRecommendationsForCompletedMission(userMission);
        } catch (Exception e) {
            log.error("유사 유저 추천 생성 실패 - userMissionId={}", userMission.getId(), e);
        }

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
                targetLon
        );

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
                .customMission(userMission.getCustomMission())
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
        if (userMission.getMission() != null) {
            return userMission.getMission().getVerificationType();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getVerificationType();
        }
        throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
    }

    private BigDecimal getGpsLatitude(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getGpsLatitude();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getGpsLatitude();
        }
        return null;
    }

    private BigDecimal getGpsLongitude(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getGpsLongitude();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getGpsLongitude();
        }
        return null;
    }

    private Integer getGpsRadius(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getGpsRadiusMeters();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getGpsRadiusMeters();
        }
        return 100;
    }

    private Integer getRequiredMinutes(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getRequiredMinutes();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getRequiredMinutes();
        }
        return null;
    }

    private int getExpReward(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getExpReward();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getExpReward();
        }
        return 10;
    }

    private Integer getBadgeDurationDays(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getBadgeDurationDays();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getBadgeDurationDays();
        }
        return 3;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
