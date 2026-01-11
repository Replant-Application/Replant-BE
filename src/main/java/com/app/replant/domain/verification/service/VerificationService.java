package com.app.replant.domain.verification.service;

import com.app.replant.domain.badge.entity.UserBadge;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.enums.PostType;
import com.app.replant.domain.post.repository.PostLikeRepository;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.entity.MissionVerification;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.MissionVerificationRepository;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.domain.verification.dto.*;
import com.app.replant.domain.verification.enums.VerificationStatus;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserMissionRepository userMissionRepository;
    private final MissionVerificationRepository missionVerificationRepository;
    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final NotificationService notificationService;
    private final ReantRepository reantRepository;
    private final ObjectMapper objectMapper;

    public Page<VerificationPostResponse> getVerifications(VerificationStatus status, Long missionId, Long customMissionId, Pageable pageable) {
        // missionId, customMissionId 파라미터는 하위 호환성을 위해 유지하지만 실제로는 무시됨
        // 미션 정보는 userMission을 통해 조회
        return postRepository.findVerificationPostsWithFilters(status, pageable)
                .map(post -> {
                    long likeCount = postLikeRepository.countByPostId(post.getId());
                    return VerificationPostResponse.from(post, likeCount);
                });
    }

    public VerificationPostResponse getVerification(Long postId, Long userId) {
        Post post = findVerificationPostById(postId);
        long likeCount = postLikeRepository.countByPostId(postId);
        return VerificationPostResponse.from(post, likeCount);
    }

    @Transactional
    public VerificationPostResponse createVerification(Long userId, VerificationPostRequest request) {
        User user = findUserById(userId);
        UserMission userMission = userMissionRepository.findByIdAndUserId(request.getUserMissionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        // COMMUNITY 타입만 인증글 작성 가능
        if (userMission.getMission() != null &&
            userMission.getMission().getVerificationType() != com.app.replant.domain.mission.enums.VerificationType.COMMUNITY) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }

        // 이미 인증글이 있는지 확인
        if (postRepository.findByUserMissionId(request.getUserMissionId()).isPresent()) {
            throw new CustomException(ErrorCode.VERIFICATION_ALREADY_EXISTS);
        }

        // 미션 상태 확인
        if (userMission.getStatus() != UserMissionStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_VERIFIED);
        }

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_DATA);
            }
        }

        // Post.createVerificationPost() 사용 (status는 내부에서 PENDING으로 설정됨)
        Post post = Post.createVerificationPost(user, userMission, request.getContent(), imageUrlsJson);

        // UserMission 상태를 PENDING으로 변경
        userMission.updateStatus(UserMissionStatus.PENDING);

        Post saved = postRepository.save(post);
        return VerificationPostResponse.from(saved);
    }

    @Transactional
    public VerificationPostResponse updateVerification(Long postId, Long userId, VerificationPostRequest request) {
        Post post = findVerificationPostById(postId);

        // NPE 방어: User null 체크
        if (post.getUser() == null || !post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // PENDING 상태일 때만 수정 가능
        if (post.getStatus() != VerificationStatus.PENDING) {
            throw new CustomException(ErrorCode.MODIFICATION_NOT_ALLOWED);
        }

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_DATA);
            }
        }

        post.updateVerificationContent(request.getContent(), imageUrlsJson);
        return VerificationPostResponse.from(post);
    }

    @Transactional
    public void deleteVerification(Long postId, Long userId) {
        Post post = findVerificationPostById(postId);

        // NPE 방어: User null 체크
        if (post.getUser() == null || !post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        if (post.getStatus() != VerificationStatus.PENDING) {
            throw new CustomException(ErrorCode.DELETION_NOT_ALLOWED);
        }

        // UserMission 상태를 다시 ASSIGNED로 변경
        post.getUserMission().updateStatus(UserMissionStatus.ASSIGNED);

        postRepository.delete(post);
    }

    // ========================================
    // 인증 완료 공통 로직 (좋아요/GPS/TIME 인증에서 사용)
    // ========================================

    /**
     * 인증 완료 처리 (공통)
     * - MissionVerification 생성
     * - 뱃지 발급
     * - 경험치 보상
     * - 알림 발송
     */
    @Transactional
    public void completeVerification(UserMission userMission, Post post) {
        User user = userMission.getUser();

        // UserMission 상태 완료
        userMission.complete();
        log.info("UserMission 완료 처리 - userMissionId={}", userMission.getId());

        // MissionVerification 생성
        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .post(post)
                .verifiedAt(LocalDateTime.now())
                .build();
        missionVerificationRepository.save(verification);

        // 뱃지 발급
        createBadge(userMission);

        // 경험치 보상
        int expReward = getExpReward(userMission);
        reantRepository.findByUserId(user.getId())
                .ifPresent(reant -> reant.addExp(expReward));

        // 알림 발송
        sendVerificationApprovedNotification(user, userMission);

        log.info("인증 완료 - userId={}, userMissionId={}, expReward={}",
                user.getId(), userMission.getId(), expReward);
    }

    /**
     * 인증 완료 처리 (Post 없는 경우 - GPS/TIME 인증용)
     */
    @Transactional
    public void completeVerification(UserMission userMission) {
        completeVerification(userMission, null);
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    private Post findVerificationPostById(Long postId) {
        return postRepository.findVerificationPostById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void createBadge(UserMission userMission) {
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

        userBadgeRepository.save(badge);
        log.info("뱃지 발급 완료 - userId={}, userMissionId={}", userMission.getUser().getId(), userMission.getId());
    }

    private Integer getBadgeDurationDays(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getBadgeDurationDays();
        }
        return 3;
    }

    private int getExpReward(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getExpReward();
        }
        return 10;
    }

    private void sendVerificationApprovedNotification(User user, UserMission userMission) {
        String missionTitle = getMissionTitle(userMission);

        notificationService.createAndPushNotification(
                user,
                NotificationType.VERIFICATION_APPROVED,
                "미션 인증이 승인되었습니다!",
                String.format("'%s' 미션 인증이 커뮤니티에서 승인되었습니다. 뱃지와 경험치를 획득했습니다!", missionTitle),
                "USER_MISSION",
                userMission.getId()
        );
    }

    private String getMissionTitle(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getTitle();
        }
        return "미션";
    }

    // GPS 인증
    @Transactional
    public Map<String, Object> verifyByGps(Long userId, Long userMissionId, Double latitude, Double longitude) {
        User user = findUserById(userId);
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        // GPS 타입만 GPS 인증 가능
        if (userMission.getMission() != null &&
            userMission.getMission().getVerificationType() != com.app.replant.domain.mission.enums.VerificationType.GPS) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }

        if (userMission.getStatus() != UserMissionStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_VERIFIED);
        }

        // GPS 거리 검증 (현재 GPS 인증 미사용)
        java.math.BigDecimal targetLat = null;
        java.math.BigDecimal targetLng = null;
        Integer radiusMeters = null;

        if (userMission.getMission() == null) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
        }

        if (targetLat != null && targetLng != null) {
            int distance = calculateDistance(
                    java.math.BigDecimal.valueOf(latitude),
                    java.math.BigDecimal.valueOf(longitude),
                    targetLat,
                    targetLng
            );

            int allowedRadius = radiusMeters != null ? radiusMeters : 100;

            if (distance > allowedRadius) {
                log.warn("GPS 인증 실패 - 거리 초과: userId={}, distance={}m, allowedRadius={}m",
                        userId, distance, allowedRadius);
                throw new CustomException(ErrorCode.GPS_OUT_OF_RANGE,
                        String.format("목표 위치에서 %dm 떨어져 있습니다. (허용 범위: %dm)", distance, allowedRadius));
            }

            log.info("GPS 거리 검증 성공 - distance={}m, allowedRadius={}m", distance, allowedRadius);
        }

        // GPS 인증 성공 처리 - 공통 로직 사용
        completeVerification(userMission);

        log.info("GPS 인증 완료 - userId={}, userMissionId={}, lat={}, lng={}", userId, userMissionId, latitude, longitude);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "GPS 인증이 완료되었습니다.");
        result.put("expReward", getExpReward(userMission));
        return result;
    }

    private int calculateDistance(java.math.BigDecimal lat1, java.math.BigDecimal lon1,
                                  java.math.BigDecimal lat2, java.math.BigDecimal lon2) {
        final int EARTH_RADIUS = 6371000;

        double dLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double dLon = Math.toRadians(lon2.subtract(lon1).doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1.doubleValue())) *
                Math.cos(Math.toRadians(lat2.doubleValue())) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) (EARTH_RADIUS * c);
    }

    // 시간 인증
    @Transactional
    public Map<String, Object> verifyByTime(Long userId, Long userMissionId, LocalDateTime startedAt, LocalDateTime endedAt) {
        User user = findUserById(userId);
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        // TIME 타입만 시간 인증 가능
        if (userMission.getMission() != null &&
            userMission.getMission().getVerificationType() != com.app.replant.domain.mission.enums.VerificationType.TIME) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }

        if (userMission.getStatus() != UserMissionStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_VERIFIED);
        }

        Integer requiredMinutes;
        if (userMission.getMission() != null) {
            requiredMinutes = userMission.getMission().getRequiredMinutes();
        } else {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
        }

        if (startedAt != null && endedAt != null) {
            if (endedAt.isBefore(startedAt)) {
                throw new CustomException(ErrorCode.INVALID_TIME_DATA, "종료 시간이 시작 시간보다 이전입니다.");
            }

            java.time.Duration duration = java.time.Duration.between(startedAt, endedAt);
            int actualMinutes = (int) duration.toMinutes();

            if (requiredMinutes != null && requiredMinutes > 0) {
                if (actualMinutes < requiredMinutes) {
                    log.warn("시간 인증 실패 - 시간 부족: userId={}, actualMinutes={}분, requiredMinutes={}분",
                            userId, actualMinutes, requiredMinutes);
                    throw new CustomException(ErrorCode.TIME_NOT_ENOUGH,
                            String.format("실제 시간: %d분, 필요 시간: %d분", actualMinutes, requiredMinutes));
                }
            }

            log.info("시간 검증 성공 - actualMinutes={}분, requiredMinutes={}분", actualMinutes, requiredMinutes);
        }

        // 시간 인증 성공 처리 - 공통 로직 사용
        completeVerification(userMission);

        log.info("시간 인증 완료 - userId={}, userMissionId={}", userId, userMissionId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "시간 인증이 완료되었습니다.");
        result.put("expReward", getExpReward(userMission));
        return result;
    }

    @Transactional
    public Map<String, Object> verifyByTime(Long userId, Long userMissionId) {
        return verifyByTime(userId, userMissionId, null, null);
    }
}
