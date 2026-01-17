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
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.domain.usermission.dto.*;
import com.app.replant.domain.usermission.entity.MissionVerification;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.MissionVerificationRepository;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.domain.missionset.entity.TodoListMission;
import com.app.replant.domain.missionset.repository.TodoListMissionRepository;
import com.app.replant.domain.missionset.repository.TodoListRepository;
import com.app.replant.domain.post.repository.PostRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final TodoListMissionRepository todoListMissionRepository;
    private final TodoListRepository todoListRepository;
    private final PostRepository postRepository;

    public Page<UserMissionResponse> getUserMissions(Long userId, Pageable pageable) {
        return userMissionRepository.findByUserIdWithFilters(userId, pageable)
                .map(UserMissionResponse::from);
    }

    public UserMissionResponse getUserMission(Long userMissionId, Long userId) {
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));
        return UserMissionResponse.from(userMission);
    }

    /**
     * 현재 사용자의 ASSIGNED 상태인 기상 미션 ID 찾기
     * @param userId 사용자 ID
     * @return userMissionId (없으면 null)
     */
    @Transactional(readOnly = true)
    public Long findCurrentWakeUpMissionId(Long userId) {
        Page<UserMission> missionsPage = userMissionRepository.findByUserIdWithFilters(
                userId, 
                org.springframework.data.domain.PageRequest.of(0, 10)
        );
        
        return missionsPage.getContent().stream()
                .filter(UserMission::isSpontaneousMission)
                .filter(um -> um.getStatus() == UserMissionStatus.ASSIGNED)
                .filter(um -> {
                    Mission mission = um.getMission();
                    if (mission == null) return false;
                    String title = mission.getTitle();
                    return title != null && (title.contains("기상") || title.contains("일어나"));
                })
                .map(UserMission::getId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 현재 사용자의 기상 미션 상태 조회
     * @param userId 사용자 ID
     * @return WakeUpMissionStatusResponse (미션이 없으면 null)
     */
    @Transactional(readOnly = true)
    public WakeUpMissionStatusResponse getCurrentWakeUpMissionStatus(Long userId) {
        Page<UserMission> missionsPage = userMissionRepository.findByUserIdWithFilters(
                userId, 
                org.springframework.data.domain.PageRequest.of(0, 10)
        );
        
        Optional<UserMission> wakeUpMission = missionsPage.getContent().stream()
                .filter(UserMission::isSpontaneousMission)
                .filter(um -> um.getStatus() == UserMissionStatus.ASSIGNED)
                .filter(um -> {
                    Mission mission = um.getMission();
                    if (mission == null) return false;
                    String title = mission.getTitle();
                    return title != null && (title.contains("기상") || title.contains("일어나"));
                })
                .findFirst();
        
        if (wakeUpMission.isEmpty()) {
            return null;
        }
        
        UserMission userMission = wakeUpMission.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime assignedAt = userMission.getAssignedAt();
        Duration elapsed = Duration.between(assignedAt, now);
        long elapsedSeconds = elapsed.toSeconds();
        long remainingSeconds = Math.max(0, 600 - elapsedSeconds); // 10분 = 600초
        
        boolean canVerify = elapsedSeconds < 600; // 10분 미만이면 인증 가능
        String message;
        
        if (!canVerify) {
            message = "인증 시간(10분)이 지났습니다.";
        } else if (remainingSeconds > 0) {
            long remainingMinutes = remainingSeconds / 60;
            message = String.format("인증 가능합니다. 남은 시간: %d분", remainingMinutes);
        } else {
            message = "인증 가능합니다.";
        }
        
        return WakeUpMissionStatusResponse.from(
                userMission.getId(),
                assignedAt,
                remainingSeconds,
                canVerify,
                message
        );
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
        int expReward = getExpReward(userMission);

        if (expReward > 0) {  // 커스텀 미션은 0 반환
            reantRepository.findByUserId(userMission.getUser().getId())
                    .ifPresent(reant -> reant.addExp(expReward));
        }

        // 뱃지 발급
        createBadge(userMission);

        // 투두리스트에 포함된 미션이면 TodoListMission도 완료 처리
        if (userMission.getMission() != null) {
            Long missionId = userMission.getMission().getId();
            Long userId = userMission.getUser().getId();
            
            // 해당 사용자의 투두리스트에서 이 미션을 찾기 (미완료 상태만)
            List<TodoListMission> todoListMissions = todoListMissionRepository
                    .findIncompleteByUserIdAndMissionId(userId, missionId);
            
            for (TodoListMission todoListMission : todoListMissions) {
                // TodoListMission 완료 처리
                if (!todoListMission.isCompletedMission()) {
                    todoListMission.complete();
                    
                    // TodoList의 completedCount 증가
                    var todoList = todoListMission.getTodoList();
                    todoList.incrementCompletedCount();
                    
                    // 변경사항 저장
                    todoListMissionRepository.save(todoListMission);
                    todoListRepository.save(todoList);
                    
                    log.info("TodoListMission 자동 완료 처리: todoListId={}, missionId={}, userId={}", 
                            todoList.getId(), missionId, userId);
                }
            }
        }

        log.info("Social Verification Completed: userMissionId={}, userId={}", 
                userMission.getId(), userMission.getUser().getId());
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
                .map(userMission -> {
                    // 완료 날짜 조회
                    LocalDateTime completedAt = null;
                    
                    // 1. MissionVerification에서 verifiedAt 조회 (GPS/TIME 인증)
                    MissionVerification verification = verificationRepository.findByUserMission(userMission)
                            .orElse(null);
                    if (verification != null) {
                        completedAt = verification.getVerifiedAt();
                    } else {
                        // 2. Post에서 verifiedAt 조회 (COMMUNITY 인증)
                        completedAt = postRepository.findByUserMissionId(userMission.getId())
                                .map(post -> post.getVerifiedAt())
                                .orElse(null);
                    }
                    
                    // UserMissionResponse 생성 (completedAt 포함)
                    return UserMissionResponse.from(userMission, completedAt);
                });
    }

    /**
     * 특정 날짜에 할당된 미션 조회 (캘린더용 - 상태 무관)
     * @param userId 사용자 ID
     * @param date 조회할 날짜 (YYYY-MM-DD 형식)
     * @return 해당 날짜에 할당된 모든 미션 (ASSIGNED, PENDING, COMPLETED 등 상태 무관)
     */
    public List<UserMissionResponse> getMissionsByDate(Long userId, LocalDate date) {
        List<UserMission> missions = userMissionRepository.findByUserIdAndAssignedDate(userId, date);
        return missions.stream()
                .map(UserMissionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 날짜 범위에 할당된 미션 조회 (캘린더용 - 상태 무관)
     * @param userId 사용자 ID
     * @param startDate 시작 날짜 (YYYY-MM-DD 형식, 포함)
     * @param endDate 종료 날짜 (YYYY-MM-DD 형식, 포함)
     * @return 해당 기간에 할당된 모든 미션 (ASSIGNED, PENDING, COMPLETED 등 상태 무관)
     */
    public List<UserMissionResponse> getMissionsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<UserMission> missions = userMissionRepository.findByUserIdAndAssignedDateRange(userId, startDate, endDate);
        return missions.stream()
                .map(UserMissionResponse::from)
                .collect(Collectors.toList());
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

    /**
     * 돌발 미션 인증
     * - 기상 미션: 시간 제한 인증 (10분 안에 버튼 클릭)
     * - 식사 미션: 게시글 작성으로 인증
     */
    @Transactional
    public VerifyMissionResponse verifySpontaneousMission(Long userMissionId, Long userId, 
                                                          VerifySpontaneousMissionRequest request) {
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        // 돌발 미션이 아니면 일반 인증으로 처리
        if (!userMission.isSpontaneousMission()) {
            throw new CustomException(ErrorCode.INVALID_MISSION_TYPE);
        }

        if (userMission.getStatus() != UserMissionStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_VERIFIED);
        }

        LocalDateTime now = LocalDateTime.now();
        Mission mission = userMission.getMission();
        
        // 기상 미션: 시간 제한 인증 (10분)
        if (mission != null && (mission.getTitle().contains("기상") || mission.getTitle().contains("일어나"))) {
            return verifyWakeUpMission(userMission, now);
        }
        
        // 식사 미션: 게시글 작성 인증
        if (request.getPostId() != null) {
            return verifyMealMission(userMission, request.getPostId(), userId, now);
        } else {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE, "식사 미션은 게시글 ID가 필요합니다.");
        }
    }

    /**
     * 기상 미션 인증 (시간 제한: 10분)
     */
    private VerifyMissionResponse verifyWakeUpMission(UserMission userMission, LocalDateTime now) {
        // 할당 시간으로부터 10분 경과 여부 확인
        LocalDateTime assignedAt = userMission.getAssignedAt();
        Duration elapsed = Duration.between(assignedAt, now);
        
        // 정확히 10분(600초) 이상 경과했는지 확인
        // 10분 0초는 아직 가능, 10분 1초부터 실패
        if (elapsed.toSeconds() >= 600) {
            // 10분 초과 시 실패 처리
            userMission.fail();
            userMissionRepository.save(userMission);
            throw new CustomException(ErrorCode.SPONTANEOUS_MISSION_TIME_EXPIRED, 
                    "기상 미션 인증 시간(10분)이 초과되었습니다.");
        }

        // 인증 성공 처리
        userMission.complete();
        
        // 경험치 지급 (10점)
        int expReward = 10;
        User user = userMission.getUser();
        Reant reant = reantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.REANT_NOT_FOUND));
        reant.addExp(expReward);

        // 뱃지 발급
        UserBadge badge = createBadge(userMission);

        // 인증 기록 생성
        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .verifiedAt(now)
                .build();
        verificationRepository.save(verification);

        log.info("기상 미션 인증 완료: userMissionId={}, userId={}, elapsedMinutes={}", 
                userMission.getId(), user.getId(), elapsed.toMinutes());

        return buildVerifyResponse(userMission, verification, expReward, badge);
    }

    /**
     * 식사 미션 인증 (게시글 작성)
     */
    private VerifyMissionResponse verifyMealMission(UserMission userMission, Long postId, 
                                                    Long userId, LocalDateTime now) {
        // 게시글 조회 및 소유자 확인
        Post post = postRepository.findByIdAndDelFlagFalse(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // 인증 성공 처리
        userMission.complete();
        
        // 경험치 지급 (10점)
        int expReward = 10;
        User user = userMission.getUser();
        Reant reant = reantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.REANT_NOT_FOUND));
        reant.addExp(expReward);

        // 뱃지 발급
        UserBadge badge = createBadge(userMission);

        // 인증 기록 생성 (게시글 연결)
        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .post(post)
                .verifiedAt(now)
                .build();
        verificationRepository.save(verification);

        log.info("식사 미션 인증 완료: userMissionId={}, userId={}, postId={}", 
                userMission.getId(), user.getId(), postId);

        return buildVerifyResponse(userMission, verification, expReward, badge);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
