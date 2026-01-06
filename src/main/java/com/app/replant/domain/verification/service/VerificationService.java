package com.app.replant.domain.verification.service;

import com.app.replant.domain.badge.entity.UserBadge;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.entity.MissionVerification;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.MissionVerificationRepository;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.domain.verification.dto.*;
import com.app.replant.domain.verification.entity.VerificationPost;
import com.app.replant.domain.verification.entity.VerificationVote;
import com.app.replant.domain.verification.enums.VerificationStatus;
import com.app.replant.domain.verification.repository.VerificationPostRepository;
import com.app.replant.domain.verification.repository.VerificationVoteRepository;
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
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationPostRepository verificationPostRepository;
    private final VerificationVoteRepository verificationVoteRepository;
    private final UserMissionRepository userMissionRepository;
    private final MissionVerificationRepository missionVerificationRepository;
    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final NotificationService notificationService;
    private final ReantRepository reantRepository;
    private final ObjectMapper objectMapper;

    public Page<VerificationPostResponse> getVerifications(VerificationStatus status, Long missionId, Long customMissionId, Pageable pageable) {
        return verificationPostRepository.findWithFilters(status, missionId, customMissionId, pageable)
                .map(VerificationPostResponse::from);
    }

    public VerificationPostResponse getVerification(Long verificationId, Long userId) {
        VerificationPost post = findVerificationById(verificationId);

        String myVote = null;
        if (userId != null) {
            myVote = verificationVoteRepository.findByVerificationPostIdAndVoterId(verificationId, userId)
                    .map(vote -> vote.getVote().name())
                    .orElse(null);
        }

        return VerificationPostResponse.from(post, myVote);
    }

    @Transactional
    public VerificationPostResponse createVerification(Long userId, VerificationPostRequest request) {
        User user = findUserById(userId);
        UserMission userMission = userMissionRepository.findByIdAndUserId(request.getUserMissionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        // COMMUNITY 타입만 VerificationPost 작성 가능
        if (userMission.getMission() != null &&
            userMission.getMission().getVerificationType() != com.app.replant.domain.mission.enums.VerificationType.COMMUNITY) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }
        if (userMission.getCustomMission() != null &&
            userMission.getCustomMission().getVerificationType() != com.app.replant.domain.mission.enums.VerificationType.COMMUNITY) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }

        // 이미 인증글이 있는지 확인
        if (verificationPostRepository.existsByUserMissionId(request.getUserMissionId())) {
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

        VerificationPost post = VerificationPost.builder()
                .user(user)
                .userMission(userMission)
                .content(request.getContent())
                .imageUrls(imageUrlsJson)
                .status(VerificationStatus.PENDING)
                .build();

        // UserMission 상태를 PENDING으로 변경
        userMission.updateStatus(UserMissionStatus.PENDING);

        VerificationPost saved = verificationPostRepository.save(post);
        return VerificationPostResponse.from(saved);
    }

    @Transactional
    public VerificationPostResponse updateVerification(Long verificationId, Long userId, VerificationPostRequest request) {
        VerificationPost post = findVerificationById(verificationId);

        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // PENDING 상태일 때만 수정 가능 (인증 통과 전에만 수정 가능)
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

        post.updateContent(request.getContent(), imageUrlsJson);
        return VerificationPostResponse.from(post);
    }

    @Transactional
    public void deleteVerification(Long verificationId, Long userId) {
        VerificationPost post = findVerificationById(verificationId);

        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        if (post.getStatus() != VerificationStatus.PENDING) {
            throw new CustomException(ErrorCode.DELETION_NOT_ALLOWED);
        }

        // UserMission 상태를 다시 ASSIGNED로 변경
        post.getUserMission().updateStatus(UserMissionStatus.ASSIGNED);

        verificationPostRepository.delete(post);
    }

    @Transactional
    public VoteResponse vote(Long verificationId, Long userId, VoteRequest request) {
        VerificationPost post = findVerificationById(verificationId);
        User voter = findUserById(userId);

        // 본인 글 투표 방지
        if (post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.SELF_VOTE_NOT_ALLOWED);
        }

        // 중복 투표 방지
        if (verificationVoteRepository.existsByVerificationPostIdAndVoterId(verificationId, userId)) {
            throw new CustomException(ErrorCode.ALREADY_VOTED);
        }

        // PENDING 상태만 투표 가능
        if (post.getStatus() != VerificationStatus.PENDING) {
            throw new CustomException(ErrorCode.VOTING_NOT_ALLOWED);
        }

        // 투표 저장
        VerificationVote vote = VerificationVote.builder()
                .verificationPost(post)
                .voter(voter)
                .vote(request.getVote())
                .build();

        verificationVoteRepository.save(vote);

        // 투표 알림 발송 (본인이 아닌 경우)
        sendVoteNotification(post.getUser(), voter, post, request.getVote());

        // 카운트 증가 및 상태 변경 (Entity 메서드 활용)
        boolean isApprove = request.getVote() == VerificationVote.VoteType.APPROVE;
        post.addVote(isApprove);

        // 승인되었을 경우 UserMission 완료 처리 및 MissionVerification 생성
        if (post.getStatus() == VerificationStatus.APPROVED) {
            UserMission userMission = post.getUserMission();
            userMission.updateStatus(UserMissionStatus.COMPLETED);

            // MissionVerification 생성 (COMMUNITY 타입은 VerificationPost 연결)
            MissionVerification verification = MissionVerification.builder()
                    .userMission(userMission)
                    .verificationPost(post)
                    .verifiedAt(LocalDateTime.now())
                    .build();
            missionVerificationRepository.save(verification);

            // 뱃지 발급
            createBadge(userMission);

            // 경험치 보상
            int expReward = getExpReward(userMission);
            reantRepository.findByUserId(post.getUser().getId())
                    .ifPresent(reant -> reant.addExp(expReward));

            // 인증 완료 알림 발송
            sendVerificationApprovedNotification(post.getUser(), userMission);

            log.info("커뮤니티 인증 승인 완료 - userId={}, userMissionId={}", post.getUser().getId(), userMission.getId());
        }

        // 거절되었을 경우 알림 발송
        if (post.getStatus() == VerificationStatus.REJECTED) {
            sendVerificationRejectedNotification(post.getUser(), post.getUserMission());
        }

        String message;
        if (post.getStatus() == VerificationStatus.APPROVED) {
            message = "인증이 승인되었습니다.";
        } else if (post.getStatus() == VerificationStatus.REJECTED) {
            message = "인증이 거절되었습니다.";
        } else {
            message = "투표가 완료되었습니다.";
        }

        return VoteResponse.builder()
                .verificationId(verificationId)
                .vote(request.getVote())
                .approveCount(post.getApproveCount())
                .rejectCount(post.getRejectCount())
                .status(post.getStatus())
                .message(message)
                .build();
    }

    private VerificationPost findVerificationById(Long verificationId) {
        return verificationPostRepository.findById(verificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 커뮤니티 인증 완료 시 뱃지 발급
     */
    private void createBadge(UserMission userMission) {
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

        userBadgeRepository.save(badge);
        log.info("뱃지 발급 완료 - userId={}, userMissionId={}", userMission.getUser().getId(), userMission.getId());
    }

    private Integer getBadgeDurationDays(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getBadgeDurationDays();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getBadgeDurationDays();
        }
        return 3;
    }

    private int getExpReward(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getExpReward();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getExpReward();
        }
        return 10;
    }

    /**
     * 인증 승인 알림 발송 (SSE 실시간 푸시 포함)
     */
    private void sendVerificationApprovedNotification(User user, UserMission userMission) {
        String missionTitle = getMissionTitle(userMission);

        notificationService.createAndPushNotification(
                user,
                "VERIFICATION_APPROVED",
                "미션 인증이 승인되었습니다!",
                String.format("'%s' 미션 인증이 커뮤니티에서 승인되었습니다. 뱃지와 경험치를 획득했습니다!", missionTitle),
                "USER_MISSION",
                userMission.getId()
        );
    }

    /**
     * 인증 거절 알림 발송 (SSE 실시간 푸시 포함)
     */
    private void sendVerificationRejectedNotification(User user, UserMission userMission) {
        String missionTitle = getMissionTitle(userMission);

        notificationService.createAndPushNotification(
                user,
                "VERIFICATION_REJECTED",
                "미션 인증이 거절되었습니다",
                String.format("'%s' 미션 인증이 커뮤니티에서 거절되었습니다. 다시 인증을 시도해주세요.", missionTitle),
                "USER_MISSION",
                userMission.getId()
        );
    }

    private String getMissionTitle(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getTitle();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getTitle();
        }
        return "미션";
    }

    /**
     * 투표 알림 발송 (본인 글에 투표 시 알림 안 함 - 이미 위에서 체크됨)
     */
    private void sendVoteNotification(User postAuthor, User voter, VerificationPost post, VerificationVote.VoteType voteType) {
        String missionTitle = getMissionTitle(post.getUserMission());
        String voteAction = voteType == VerificationVote.VoteType.APPROVE ? "응원" : "반대";

        String title = "인증글에 투표가 있습니다";
        String content = String.format("%s님이 '%s' 인증글에 %s 투표를 했습니다.",
                voter.getNickname(), truncateMissionTitle(missionTitle, 15), voteAction);

        notificationService.createAndPushNotification(
                postAuthor,
                "VOTE",
                title,
                content,
                "VERIFICATION",
                post.getId()
        );

        log.info("투표 알림 발송 - verificationId={}, voterId={}, postAuthorId={}, voteType={}",
                post.getId(), voter.getId(), postAuthor.getId(), voteType);
    }

    /**
     * 미션 제목 자르기
     */
    private String truncateMissionTitle(String title, int maxLength) {
        if (title == null) return "미션";
        if (title.length() <= maxLength) return title;
        return title.substring(0, maxLength) + "...";
    }

    /**
     * GPS 인증 (GPS 타입 미션)
     * Haversine 공식을 사용하여 거리 검증
     */
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
        if (userMission.getCustomMission() != null &&
            userMission.getCustomMission().getVerificationType() != com.app.replant.domain.mission.enums.VerificationType.GPS) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }

        // 미션 상태 확인
        if (userMission.getStatus() != UserMissionStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_VERIFIED);
        }

        // GPS 거리 검증
        java.math.BigDecimal targetLat;
        java.math.BigDecimal targetLng;
        Integer radiusMeters;

        if (userMission.getMission() != null) {
            targetLat = userMission.getMission().getGpsLatitude();
            targetLng = userMission.getMission().getGpsLongitude();
            radiusMeters = userMission.getMission().getGpsRadiusMeters();
        } else if (userMission.getCustomMission() != null) {
            targetLat = userMission.getCustomMission().getGpsLatitude();
            targetLng = userMission.getCustomMission().getGpsLongitude();
            radiusMeters = userMission.getCustomMission().getGpsRadiusMeters();
        } else {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
        }

        // 목표 위치가 설정되어 있는 경우 거리 검증
        if (targetLat != null && targetLng != null) {
            int distance = calculateDistance(
                    java.math.BigDecimal.valueOf(latitude),
                    java.math.BigDecimal.valueOf(longitude),
                    targetLat,
                    targetLng
            );

            int allowedRadius = radiusMeters != null ? radiusMeters : 100; // 기본값 100m

            if (distance > allowedRadius) {
                log.warn("GPS 인증 실패 - 거리 초과: userId={}, distance={}m, allowedRadius={}m",
                        userId, distance, allowedRadius);
                throw new CustomException(ErrorCode.GPS_OUT_OF_RANGE,
                        String.format("목표 위치에서 %dm 떨어져 있습니다. (허용 범위: %dm)", distance, allowedRadius));
            }

            log.info("GPS 거리 검증 성공 - distance={}m, allowedRadius={}m", distance, allowedRadius);
        }

        // GPS 인증 성공 처리
        userMission.updateStatus(UserMissionStatus.COMPLETED);

        // MissionVerification 생성
        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .verifiedAt(LocalDateTime.now())
                .build();
        missionVerificationRepository.save(verification);

        // 뱃지 발급
        createBadge(userMission);

        // 경험치 보상
        int expReward = getExpReward(userMission);
        reantRepository.findByUserId(userId)
                .ifPresent(reant -> reant.addExp(expReward));

        // 알림 발송
        sendVerificationApprovedNotification(user, userMission);

        log.info("GPS 인증 완료 - userId={}, userMissionId={}, lat={}, lng={}", userId, userMissionId, latitude, longitude);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "GPS 인증이 완료되었습니다.");
        result.put("expReward", expReward);
        return result;
    }

    /**
     * Haversine 공식을 사용한 두 지점 간 거리 계산 (미터 단위)
     */
    private int calculateDistance(java.math.BigDecimal lat1, java.math.BigDecimal lon1,
                                  java.math.BigDecimal lat2, java.math.BigDecimal lon2) {
        final int EARTH_RADIUS = 6371000; // 지구 반경 (미터)

        double dLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double dLon = Math.toRadians(lon2.subtract(lon1).doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1.doubleValue())) *
                Math.cos(Math.toRadians(lat2.doubleValue())) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) (EARTH_RADIUS * c);
    }

    /**
     * 시간 인증 (TIME 타입 미션)
     * 시작/종료 시간을 받아 소요 시간 검증
     */
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
        if (userMission.getCustomMission() != null &&
            userMission.getCustomMission().getVerificationType() != com.app.replant.domain.mission.enums.VerificationType.TIME) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE);
        }

        // 미션 상태 확인
        if (userMission.getStatus() != UserMissionStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_VERIFIED);
        }

        // 시간 검증
        Integer requiredMinutes;
        if (userMission.getMission() != null) {
            requiredMinutes = userMission.getMission().getRequiredMinutes();
        } else if (userMission.getCustomMission() != null) {
            requiredMinutes = userMission.getCustomMission().getRequiredMinutes();
        } else {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
        }

        // 시작/종료 시간이 전달된 경우 시간 검증
        if (startedAt != null && endedAt != null) {
            // 종료 시간이 시작 시간보다 이전인 경우
            if (endedAt.isBefore(startedAt)) {
                throw new CustomException(ErrorCode.INVALID_TIME_DATA,
                        "종료 시간이 시작 시간보다 이전입니다.");
            }

            java.time.Duration duration = java.time.Duration.between(startedAt, endedAt);
            int actualMinutes = (int) duration.toMinutes();

            // 필요 시간이 설정되어 있는 경우 검증
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

        // 시간 인증 성공 처리
        userMission.updateStatus(UserMissionStatus.COMPLETED);

        // MissionVerification 생성
        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .verifiedAt(LocalDateTime.now())
                .build();
        missionVerificationRepository.save(verification);

        // 뱃지 발급
        createBadge(userMission);

        // 경험치 보상
        int expReward = getExpReward(userMission);
        reantRepository.findByUserId(userId)
                .ifPresent(reant -> reant.addExp(expReward));

        // 알림 발송
        sendVerificationApprovedNotification(user, userMission);

        log.info("시간 인증 완료 - userId={}, userMissionId={}", userId, userMissionId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "시간 인증이 완료되었습니다.");
        result.put("expReward", expReward);
        return result;
    }

    /**
     * 시간 인증 (TIME 타입 미션) - 단순 인증 (시간 파라미터 없이)
     * 하위 호환성을 위해 유지
     */
    @Transactional
    public Map<String, Object> verifyByTime(Long userId, Long userMissionId) {
        return verifyByTime(userId, userMissionId, null, null);
    }
}
