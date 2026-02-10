package com.app.replant.domain.usermission.service;

import com.app.replant.domain.badge.entity.UserBadge;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.reant.service.ReantService;
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
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
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
    private final ReantService reantService;
    private final ReantRepository reantRepository;
    private final TodoListMissionRepository todoListMissionRepository;
    private final TodoListRepository todoListRepository;
    private final PostRepository postRepository;

    public Page<UserMissionResponse> getUserMissions(Long userId, Pageable pageable) {
        log.info("[나의 미션] API 요청 - userId: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        Page<UserMission> missionPage = userMissionRepository.findByUserIdWithFilters(userId, pageable);
        log.info("[나의 미션] API 응답 - userId: {}, 총 {}건 (ACTIVE 투두리스트 기준)",
                userId, missionPage.getTotalElements());
        return missionPage.map(UserMissionResponse::from);
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

    /**
     * 커스텀 미션 완료 처리 (인증 없이 즉시 완료)
     * 내 미션에 추가된 ASSIGNED 상태의 커스텀 미션만 완료 가능
     */
    @Transactional
    public UserMissionResponse completeCustomMission(Long userId, Long missionId) {
        List<UserMission> list = userMissionRepository.findByUserIdAndMissionIdAndStatusAssigned(userId, missionId);
        if (list == null || list.isEmpty()) {
            throw new CustomException(ErrorCode.USER_MISSION_NOT_FOUND, "내 미션에 추가한 후 완료할 수 있습니다.");
        }
        UserMission userMission = list.get(0);
        if (!userMission.isCustomMission()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "공식 미션은 이 방법으로 완료할 수 없습니다.");
        }
        userMission.complete();
        userMissionRepository.saveAndFlush(userMission);

        // 투두리스트에 포함된 같은 미션이 있으면 TodoListMission도 완료 처리 (동기화)
        if (userMission.getMission() != null) {
            Long umMissionId = userMission.getMission().getId();
            Long umUserId = userMission.getUser().getId();
            List<TodoListMission> todoListMissions = todoListMissionRepository
                    .findIncompleteByUserIdAndMissionId(umUserId, umMissionId);
            for (TodoListMission todoListMission : todoListMissions) {
                if (!todoListMission.isCompletedMission()) {
                    todoListMission.complete();
                    var todoList = todoListMission.getTodoList();
                    todoList.incrementCompletedCount();
                    todoListMissionRepository.save(todoListMission);
                    todoListRepository.save(todoList);
                    log.info("completeCustomMission: TodoListMission 동기화 완료 todoListId={}, missionId={}, userId={}",
                            todoList.getId(), umMissionId, umUserId);
                }
            }
        }

        return UserMissionResponse.from(userMission, LocalDateTime.now());
    }

    /**
     * 커스텀 미션 취소 (인증 취소)
     * - COMPLETED 상태인 경우: ASSIGNED로 상태 변경 (인증 취소)
     * - ASSIGNED 상태인 경우: 삭제 (내 미션에서 제거)
     */
    @Transactional
    public UserMissionResponse cancelCustomMission(Long userId, Long missionId) {
        // 상태와 관계없이 해당 사용자의 커스텀 미션 조회
        List<UserMission> list = userMissionRepository.findByUserIdAndMissionId(userId, missionId);
        if (list == null || list.isEmpty()) {
            throw new CustomException(ErrorCode.USER_MISSION_NOT_FOUND, "내 미션에 추가한 커스텀 미션만 취소할 수 있습니다.");
        }
        
        // 가장 최근의 UserMission 선택 (같은 미션이 여러 개 있을 수 있음)
        UserMission userMission = list.get(0);
        if (!userMission.isCustomMission()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "공식 미션은 이 방법으로 취소할 수 없습니다.");
        }
        
        // COMPLETED 상태인 경우: ASSIGNED로 상태 변경 (인증 취소)
        if (userMission.getStatus() == UserMissionStatus.COMPLETED) {
            userMission.updateStatus(UserMissionStatus.ASSIGNED);
            userMissionRepository.saveAndFlush(userMission);
            log.info("[커스텀 미션 인증 취소] userId: {}, missionId: {}, status: {} -> {}", 
                    userId, missionId, UserMissionStatus.COMPLETED, userMission.getStatus());
            
            // 투두리스트에 포함된 같은 미션이 있으면 TodoListMission도 완료 취소 처리
            if (userMission.getMission() != null) {
                Long umMissionId = userMission.getMission().getId();
                Long umUserId = userMission.getUser().getId();
                
                // 완료된 TodoListMission 찾기
                List<TodoListMission> completedTodoListMissions = todoListMissionRepository
                        .findCompleteByUserIdAndMissionId(umUserId, umMissionId);
                
                for (TodoListMission todoListMission : completedTodoListMissions) {
                    if (todoListMission.isCompletedMission()) {
                        todoListMission.uncomplete();
                        
                        // TodoList의 completedCount 감소
                        var todoList = todoListMission.getTodoList();
                        todoList.decrementCompletedCount();
                        
                        // 변경사항 저장
                        todoListMissionRepository.save(todoListMission);
                        todoListRepository.save(todoList);
                        
                        log.info("커스텀 미션 인증 취소로 TodoListMission 완료 취소: todoListId={}, missionId={}, userId={}", 
                                todoList.getId(), umMissionId, umUserId);
                    }
                }
            }
            
            return UserMissionResponse.from(userMission, LocalDateTime.now());
        }
        
        // ASSIGNED 상태인 경우: 삭제 (내 미션에서 제거)
        userMissionRepository.delete(userMission);
        log.info("[커스텀 미션 삭제] userId: {}, missionId: {}, status: {}", userId, missionId, userMission.getStatus());
        
        // 삭제된 경우 null 반환 (클라이언트에서 처리)
        return null;
    }

    @Transactional
    public void completeMissionVerification(UserMission userMission) {
        if (userMission.getStatus() == UserMissionStatus.COMPLETED) {
            return;
        }

        // 미션 완료 처리
        userMission.updateStatus(UserMissionStatus.COMPLETED);
        // 명시적으로 저장하여 DB에 반영
        userMissionRepository.saveAndFlush(userMission);

        // 보상 지급 (커스텀 미션은 경험치 지급 없음)
        int baseExpReward = getExpReward(userMission);

        if (baseExpReward > 0) {  // 커스텀 미션은 0 반환
            // Post에서 completionRate 조회 (completion_rate에 따라 경험치 비례 지급)
            Optional<Post> postOpt = postRepository.findByUserMissionId(userMission.getId());
            Integer completionRate = postOpt.map(Post::getCompletionRate).orElse(null);
            
            // completionRate가 null이면 100%로 처리 (기존 동작 유지)
            if (completionRate == null) {
                completionRate = 100;
            }
            
            // completionRate 범위 검증 (0-100)
            if (completionRate < 0) {
                completionRate = 0;
            } else if (completionRate > 100) {
                completionRate = 100;
            }
            
            // 경험치 비례 계산 (0-100% 범위)
            int actualExpReward = (int) Math.round(baseExpReward * (completionRate / 100.0));
            
            // 경험치 지급 - N+1 문제 방지를 위해 user를 함께 fetch join
            if (actualExpReward > 0) {
                Long userId = userMission.getUser().getId();
                reantRepository.findByUserIdWithUser(userId)
                        .ifPresent(reant -> {
                            reant.addExp(actualExpReward);
                            reantRepository.save(reant);  // 변경사항 저장
                            reantService.evictReantCache(userId);  // 캐시 무효화
                        });
                
                log.info("경험치 비례 지급: userMissionId={}, baseExp={}, completionRate={}%, actualExp={}", 
                        userMission.getId(), baseExpReward, completionRate, actualExpReward);
            } else {
                log.info("경험치 지급 없음: userMissionId={}, baseExp={}, completionRate={}%, actualExp=0", 
                        userMission.getId(), baseExpReward, completionRate);
            }
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

        // 미션 완료 처리 (투두리스트 미션 완료 처리 포함)
        completeMissionVerification(userMission);

        // 보상 지급 정보 조회 (이미 completeMissionVerification에서 처리됨)
        int expReward = getExpReward(userMission);
        
        // 뱃지 조회 (이미 completeMissionVerification에서 발급됨)
        UserBadge badge = userBadgeRepository.findValidBadgeForMission(
                userId, 
                userMission.getMission().getId(), 
                LocalDateTime.now()
        ).orElse(null);

        // 유사 유저 추천 생성 (RecommendationService 삭제로 인해 임시 비활성화)
        // TODO: 추천 시스템 재구현 시 활성화
        log.debug("유사 유저 추천 기능 비활성화됨 - userMissionId={}", userMission.getId());
        return buildVerifyResponse(userMission, verification, expReward, badge);
    }

    /**
     * 시간 인증 미션 완료 (간편 버전)
     * 돌발 미션(기상/식사)과 일반 투두리스트 TIME 인증 모두 지원
     */
    @Transactional
    public VerifyMissionResponse verifyByTime(Long userMissionId, Long userId) {
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        if (userMission.getStatus() != UserMissionStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_VERIFIED);
        }

        VerificationType requiredType = getVerificationType(userMission);
        if (requiredType != VerificationType.TIME) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE, 
                    "이 미션은 시간 인증이 필요하지 않습니다. 요구 타입: " + requiredType);
        }

        // 시간 인증은 시작 시간과 종료 시간이 필요하지만, 간편 버전에서는 현재 시간을 기준으로 처리
        // 실제로는 클라이언트에서 시작/종료 시간을 전달해야 하지만, 임시로 현재 시간 사용
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = now.minusMinutes(1); // 임시: 1분 전부터 시작한 것으로 가정
        LocalDateTime endedAt = now;

        VerifyMissionRequest request = new VerifyMissionRequest();
        request.setType(VerificationType.TIME);
        request.setStartedAt(startedAt);
        request.setEndedAt(endedAt);

        MissionVerification verification = verifyTime(userMission, request);

        // 미션 완료 처리 (투두리스트 미션 완료 처리 포함)
        completeMissionVerification(userMission);

        // 보상 지급 정보 조회
        int expReward = getExpReward(userMission);
        
        // 뱃지 조회
        UserBadge badge = null;
        if (userMission.getMission() != null) {
            badge = userBadgeRepository.findValidBadgeForMission(
                    userId, 
                    userMission.getMission().getId(), 
                    LocalDateTime.now()
            ).orElse(null);
        }

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
            // 기상 미션 등 BUTTON 타입은 시간 인증(TIME)으로 처리 (1분 이상 = 인증 완료)
            if (mission.getVerificationType() == VerificationType.BUTTON) {
                return VerificationType.TIME;
            }
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
        if (mission == null) {
            return null;
        }
        // 기상 미션 등 BUTTON 타입: 1분 이상이면 인증 완료로 처리
        if (mission.getVerificationType() == VerificationType.BUTTON) {
            return mission.getRequiredMinutes() != null ? mission.getRequiredMinutes() : 1;
        }
        return mission.getRequiredMinutes();
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
        // N+1 문제 방지를 위해 reant를 함께 로드
        return userRepository.findByIdWithReant(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
