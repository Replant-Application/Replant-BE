package com.app.replant.domain.usermission.service;

import com.app.replant.domain.badge.entity.UserBadge;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.mission.repository.MissionRepository;
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
import com.app.replant.domain.notification.repository.NotificationRepository;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.app.replant.domain.notification.entity.Notification;

@Service
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
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<UserMissionResponse> getUserMissions(Long userId, Pageable pageable) {
        return userMissionRepository.findByUserIdWithFilters(userId, pageable)
                .map(userMission -> convertToUserMissionResponse(userMission));
    }
    
    /**
     * UserMission을 UserMissionResponse로 변환 (돌발 미션 처리 포함)
     */
    private UserMissionResponse convertToUserMissionResponse(UserMission userMission) {
        return convertToUserMissionResponse(userMission, null);
    }
    
    /**
     * UserMission을 UserMissionResponse로 변환 (돌발 미션 처리 포함, completedAt 포함)
     */
    private UserMissionResponse convertToUserMissionResponse(UserMission userMission, LocalDateTime completedAt) {
        // 돌발 미션이고 mission이 null인 경우 spontaneous_mission 정보 조회
        if (userMission.isSpontaneousMission() && userMission.getMission() == null) {
            // 기상 미션인지 식사 미션인지 구분하기 어려우므로, 
            // 일단 기본적인 UserMissionResponse를 생성하고 missionType만 설정
            return UserMissionResponse.builder()
                    .id(userMission.getId())
                    .missionType("OFFICIAL")
                    .mission(null)  // 돌발 미션은 mission이 null
                    .customMission(null)
                    .assignedAt(userMission.getAssignedAt())
                    .dueDate(userMission.getDueDate())
                    .status(userMission.getStatus())
                    .completedAt(completedAt)
                    .build();
        }
        
        // 일반 미션의 경우 기존 로직 사용
        return UserMissionResponse.from(userMission, completedAt);
    }

    public UserMissionResponse getUserMission(Long userMissionId, Long userId) {
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));
        return convertToUserMissionResponse(userMission);
    }

    /**
     * 현재 사용자의 ASSIGNED 상태인 기상 미션 ID 찾기
     * @param userId 사용자 ID
     * @return userMissionId (없으면 null)
     */
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    @Transactional(readOnly = true)
    public Long findCurrentWakeUpMissionId(Long userId) {
        // 오늘 날짜에 할당된 모든 미션 조회 (KST 기준 - 사용자/스케줄러와 동일)
        LocalDate today = LocalDate.now(ZONE_SEOUL);
        List<UserMission> todayMissions = userMissionRepository.findByUserIdAndAssignedDate(userId, today);
        
        // 돌발 미션 중 가장 최근 ASSIGNED 미션 조회
        java.util.Optional<UserMission> found = todayMissions.stream()
                .filter(UserMission::isSpontaneousMission)
                .filter(um -> um.getStatus() == UserMissionStatus.ASSIGNED)
                .max(java.util.Comparator.comparing(UserMission::getAssignedAt));
        if (found.isPresent()) {
            return found.get().getId();
        }
        // 폴백1: DB/JDBC 시간대 불일치 시 오늘(KST) 할당된 ASSIGNED 돌발 미션을 최신 1건으로 조회
        java.util.Optional<Long> fallbackToday = userMissionRepository.findTopByUserIdAndIsSpontaneousAndStatusOrderByAssignedAtDesc(
                        userId, true, UserMissionStatus.ASSIGNED)
                .filter(um -> java.time.ZonedDateTime.of(um.getAssignedAt(), ZONE_SEOUL).toLocalDate().equals(today))
                .map(UserMission::getId);
        if (fallbackToday.isPresent()) return fallbackToday.get();
        // 폴백2 [테스트용]: 날짜 무관, 가장 최근 ASSIGNED 돌발 미션 1건 → 알림 받은 미션은 항상 활성
        return userMissionRepository.findTopByUserIdAndIsSpontaneousAndStatusOrderByAssignedAtDesc(
                        userId, true, UserMissionStatus.ASSIGNED)
                .map(UserMission::getId)
                .orElse(null);
    }

    /**
     * 현재 사용자의 기상 미션 상태 조회
     * @param userId 사용자 ID
     * @return WakeUpMissionStatusResponse (미션이 없으면 null)
     */
    @Transactional(readOnly = true)
    public WakeUpMissionStatusResponse getCurrentWakeUpMissionStatus(Long userId) {
        log.info("[기상미션] 상태 조회 시작 userId={}", userId);
        
        // 오늘 날짜(KST)에 할당된 모든 미션 조회
        LocalDate today = LocalDate.now(ZONE_SEOUL);
        List<UserMission> todayMissions = userMissionRepository.findByUserIdAndAssignedDate(userId, today);
        log.info("[기상미션] 오늘(KST) 날짜={}, 할당된 미션 수={}", today, todayMissions.size());
        
        // 돌발 미션 중 가장 최근 ASSIGNED 미션 조회 (기상 미션은 제목에 의존하지 않음)
        Optional<UserMission> wakeUpMission = todayMissions.stream()
                .filter(UserMission::isSpontaneousMission)
                .filter(um -> um.getStatus() == UserMissionStatus.ASSIGNED)
                .max(java.util.Comparator.comparing(UserMission::getAssignedAt));
        // 폴백1: DB/JDBC 시간대 불일치 시 오늘(KST) 날짜인 ASSIGNED 돌발 미션을 최신 1건으로 조회
        if (wakeUpMission.isEmpty()) {
            wakeUpMission = userMissionRepository.findTopByUserIdAndIsSpontaneousAndStatusOrderByAssignedAtDesc(
                            userId, true, UserMissionStatus.ASSIGNED)
                    .filter(um -> java.time.ZonedDateTime.of(um.getAssignedAt(), ZONE_SEOUL).toLocalDate().equals(today));
            if (wakeUpMission.isPresent()) {
                log.info("[기상미션] 폴백으로 기상 미션 조회 성공 userId={}, userMissionId={}", userId, wakeUpMission.get().getId());
            }
        }
        // 폴백2 [테스트용]: 날짜 무관, 가장 최근 ASSIGNED 돌발 미션 → 알림 받은 미션은 항상 활성
        if (wakeUpMission.isEmpty()) {
            wakeUpMission = userMissionRepository.findTopByUserIdAndIsSpontaneousAndStatusOrderByAssignedAtDesc(
                    userId, true, UserMissionStatus.ASSIGNED);
            if (wakeUpMission.isPresent()) {
                log.info("[기상미션] 테스트 폴백: 날짜 무관 최신 기상 미션 사용 userId={}, userMissionId={}", userId, wakeUpMission.get().getId());
            }
        }
        if (wakeUpMission.isEmpty()) {
            log.info("[기상미션] ASSIGNED 기상 미션 없음 userId={}, todayMissions={}", userId, todayMissions.size());
            return null;
        }
        
        UserMission userMission = wakeUpMission.get();
        log.info("[기상미션] 선택된 미션 userMissionId={}, assignedAt={}", userMission.getId(), userMission.getAssignedAt());
        
        User user = userMission.getUser();
        String wakeTimeStr = user.getWakeTime();
        
        if (wakeTimeStr == null || wakeTimeStr.isEmpty()) {
            log.warn("[기상미션] 사용자 wake_time 없음 userId={}", user.getId());
            return null;
        }
        
        // 알림(할당) 시점 기준 10분 이내에만 인증 가능
        final long WAKE_UP_VERIFY_WINDOW_SECONDS = 10L * 60;
        java.time.ZonedDateTime nowZ = java.time.ZonedDateTime.now(ZONE_SEOUL);
        java.time.ZonedDateTime assignedZ = userMission.getAssignedAt().atZone(ZONE_SEOUL);
        long elapsedSeconds = java.time.temporal.ChronoUnit.SECONDS.between(assignedZ, nowZ);
        long remainingSeconds = Math.max(0, WAKE_UP_VERIFY_WINDOW_SECONDS - elapsedSeconds);
        boolean canVerify = remainingSeconds > 0;
        String message = canVerify ? "10분 이내에 인증해주세요." : "10분이 지나 만료되었습니다.";
        log.info("[기상미션] 10분 윈도우 userId={}, userMissionId={}, remainingSeconds={}, canVerify={}", user.getId(), userMission.getId(), remainingSeconds, canVerify);
        
        return WakeUpMissionStatusResponse.from(
                userMission.getId(),
                userMission.getAssignedAt(),
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
     * 커스텀 미션 인증 취소 (완료 상태 → 할당 상태로 되돌림)
     * 실수로 인증 완료를 눌렀을 때 다시 체크하면 인증 취소 가능.
     * - 미션 탭에서 완료한 경우: UserMission COMPLETED → ASSIGNED + 해당 미션의 모든 TodoListMission 완료 해제
     * - 투두리스트 탭에서만 완료한 경우: 해당 미션의 TodoListMission만 완료 해제 (UserMission은 미완료 상태 유지)
     */
    @Transactional
    public UserMissionResponse cancelCustomMissionCompletion(Long userId, Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));
        if (mission.getMissionType() != MissionType.CUSTOM) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "공식 미션은 이 방법으로 인증 취소할 수 없습니다.");
        }

        UserMission userMission = null;
        List<UserMission> list = userMissionRepository.findByUserIdAndMissionId(userId, missionId);
        if (list != null && !list.isEmpty()) {
            userMission = list.stream()
                    .filter(um -> um.getStatus() == UserMissionStatus.COMPLETED)
                    .findFirst()
                    .orElse(list.get(0));
        }

        if (userMission != null && userMission.getStatus() == UserMissionStatus.COMPLETED) {
            userMission.updateStatus(UserMissionStatus.ASSIGNED);
            userMissionRepository.saveAndFlush(userMission);
        }

        // 투두리스트에 포함된 같은 미션의 완료 상태 되돌림 (미션 탭/투두 탭 구분 없이 모두 처리)
        List<TodoListMission> completedTodoMissions = todoListMissionRepository
                .findCompleteByUserIdAndMissionId(userId, missionId);
        for (TodoListMission todoListMission : completedTodoMissions) {
            todoListMission.uncomplete();
            var todoList = todoListMission.getTodoList();
            todoList.decrementCompletedCount();
            todoListMissionRepository.save(todoListMission);
            todoListRepository.save(todoList);
            log.info("cancelCustomMissionCompletion: TodoListMission 인증 취소 todoListId={}, missionId={}, userId={}",
                    todoList.getId(), missionId, userId);
        }

        if (userMission != null) {
            log.info("커스텀 미션 인증 취소: userId={}, missionId={}, userMissionId={}", userId, missionId, userMission.getId());
            return UserMissionResponse.from(userMission);
        }
        // 투두리스트에서만 완료했던 경우: UserMission 없이 TodoListMission만 되돌렸을 때
        return UserMissionResponse.builder()
                .status(UserMissionStatus.ASSIGNED)
                .missionType("CUSTOM")
                .customMission(UserMissionResponse.CustomMissionInfo.builder()
                        .id(missionId)
                        .title(mission.getTitle())
                        .build())
                .build();
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
            
            // 경험치 지급
            if (actualExpReward > 0) {
                reantRepository.findByUserId(userMission.getUser().getId())
                        .ifPresent(reant -> reant.addExp(actualExpReward));
                
                log.info("경험치 비례 지급: userMissionId={}, baseExp={}, completionRate={}%, actualExp={}", 
                        userMission.getId(), baseExpReward, completionRate, actualExpReward);
            } else {
                log.info("경험치 지급 없음: userMissionId={}, baseExp={}, completionRate={}%, actualExp=0", 
                        userMission.getId(), baseExpReward, completionRate);
            }
        }

        // 배지 발급 (기상/돌발 미션은 배지 발급하지 않음)
        if (!userMission.isSpontaneousMission()) {
            createBadge(userMission);
        }

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

        // 미션 완료 시 관련 알림 자동 읽음 처리 (돌발 미션 알림 등)
        Long userId = userMission.getUser().getId();
        Long userMissionId = userMission.getId();
        List<Notification> relatedNotifications = notificationRepository
                .findByUserIdAndReference(userId, "USER_MISSION", userMissionId);
        
        for (Notification notification : relatedNotifications) {
            if (!notification.getIsRead()) {
                notification.markAsRead();
                notificationRepository.save(notification);
                log.info("미션 완료로 인한 알림 자동 읽음 처리: notificationId={}, userMissionId={}, userId={}", 
                        notification.getId(), userMissionId, userId);
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
        
        // 배지 조회 (이미 completeMissionVerification에서 발급됨)
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
                    return convertToUserMissionResponse(userMission, completedAt);
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
                .map(um -> toUserMissionResponseWithPostId(um, null))
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
                .map(um -> toUserMissionResponseWithPostId(um, null))
                .collect(Collectors.toList());
    }

    /**
     * 캘린더용: UserMission → UserMissionResponse (완료 시 인증 게시글 ID 포함)
     */
    private UserMissionResponse toUserMissionResponseWithPostId(UserMission um, LocalDateTime completedAt) {
        Long verificationPostId = null;
        if (um.getStatus() == UserMissionStatus.COMPLETED) {
            verificationPostId = postRepository.findByUserMissionId(um.getId())
                    .map(Post::getId)
                    .orElse(null);
        }
        return UserMissionResponse.from(um, completedAt, verificationPostId);
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

        // 배지 정보 생성 (돌발 미션은 배지가 없을 수 있음)
        VerifyMissionResponse.BadgeInfo badgeInfo = null;
        if (badge != null) {
            badgeInfo = VerifyMissionResponse.BadgeInfo.builder()
                    .id(badge.getId())
                    .expiresAt(badge.getExpiresAt())
                    .build();
        }

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
     * 시간 인증 (단일 엔드포인트)
     * - 돌발 미션(기상/식사): verifySpontaneousMission
     * - 일반 투두리스트 TIME 인증: verifyMission (startedAt/endedAt 자동 생성)
     */
    @Transactional
    public VerifyMissionResponse verifyByTime(Long userMissionId, Long userId) {
        UserMission userMission = userMissionRepository.findByIdAndUserId(userMissionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_MISSION_NOT_FOUND));

        if (userMission.isSpontaneousMission()) {
            return verifySpontaneousMission(userMissionId, userId, new VerifySpontaneousMissionRequest());
        }

        VerificationType verificationType = getVerificationType(userMission);
        if (verificationType != VerificationType.TIME) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_TYPE, "시간 인증이 아닌 미션입니다.");
        }

        Integer requiredMinutes = getRequiredMinutes(userMission);
        // null 또는 0이면 "즉시 완료"로 처리 (버튼 한 번으로 완료)
        if (requiredMinutes == null || requiredMinutes <= 0) {
            requiredMinutes = 1;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = now.minusMinutes(requiredMinutes);
        VerifyMissionRequest request = new VerifyMissionRequest();
        request.setType(VerificationType.TIME);
        request.setStartedAt(startedAt);
        request.setEndedAt(now);
        return verifyMission(userMissionId, userId, request);
    }

    /**
     * 돌발 미션 인증
     * - 기상 미션: 시간 제한 인증 (1일 안에 버튼 클릭)
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
        
        // postId가 없으면 기상 미션 (버튼만 클릭), 있으면 식사 미션 (게시글 인증)
        if (request == null || request.getPostId() == null) {
            // 기상 미션: 시간 제한 인증 (1일 안에 버튼 클릭)
            return verifyWakeUpMission(userMission, now);
        } else {
            // 식사 미션: 게시글 작성 인증
            return verifyMealMission(userMission, request.getPostId(), userId, now);
        }
    }

    /**
     * 기상 미션 인증 (알림 후 10분 이내에만 가능)
     * KST 기준으로 assigned_at + 10분 초과 시 만료 처리
     */
    private VerifyMissionResponse verifyWakeUpMission(UserMission userMission, LocalDateTime now) {
        User user = userMission.getUser();
        String wakeTimeStr = user.getWakeTime();
        
        if (wakeTimeStr == null || wakeTimeStr.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_MISSION_TYPE, 
                    "사용자의 기상 시간이 설정되지 않았습니다.");
        }
        
        // 알림(할당) 후 10분 초과 시 인증 불가
        LocalDateTime nowKst = java.time.ZonedDateTime.now(ZONE_SEOUL).toLocalDateTime();
        LocalDateTime deadline = userMission.getAssignedAt().plusMinutes(10);
        if (nowKst.isAfter(deadline)) {
            log.warn("[기상미션] 만료됨 userId={}, userMissionId={}, assignedAt={}, deadline={}, now={}", 
                    user.getId(), userMission.getId(), userMission.getAssignedAt(), deadline, nowKst);
            throw new CustomException(ErrorCode.MISSION_EXPIRED, "알림 후 10분이 지나 인증할 수 없습니다.");
        }

        // 인증 성공 처리 (투두리스트 미션 완료 처리 포함)
        completeMissionVerification(userMission);

        int expReward = 10;

        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .verifiedAt(now)
                .build();
        verificationRepository.save(verification);

        log.info("[기상미션] 인증 성공 userMissionId={}, userId={}", userMission.getId(), user.getId());

        return buildVerifyResponse(userMission, verification, expReward, null);
    }

    /**
     * 식사 미션 인증 (게시글 작성)
     * 사용자 설정 식사 시간 + 2시간 이내에만 인증 가능
     */
    private VerifyMissionResponse verifyMealMission(UserMission userMission, Long postId, 
                                                    Long userId, LocalDateTime now) {
        User user = userMission.getUser();
        Mission mission = userMission.getMission();
        
        if (mission == null) {
            throw new CustomException(ErrorCode.INVALID_MISSION_TYPE, 
                    "미션 정보를 찾을 수 없습니다.");
        }
        
        // 미션 제목으로 식사 타입 구분
        String missionTitle = mission.getTitle();
        String mealTimeStr = null;
        String mealType = null;
        
        if (missionTitle.contains("아침")) {
            mealTimeStr = user.getBreakfastTime();
            mealType = "아침";
        } else if (missionTitle.contains("점심")) {
            mealTimeStr = user.getLunchTime();
            mealType = "점심";
        } else if (missionTitle.contains("저녁")) {
            mealTimeStr = user.getDinnerTime();
            mealType = "저녁";
        } else {
            throw new CustomException(ErrorCode.INVALID_MISSION_TYPE, 
                    "식사 미션 타입을 확인할 수 없습니다.");
        }
        
        if (mealTimeStr == null || mealTimeStr.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_MISSION_TYPE, 
                    String.format("사용자의 %s 식사 시간이 설정되지 않았습니다.", mealType));
        }
        
        // 사용자 설정 식사 시간 파싱
        LocalTime mealTime;
        try {
            // 다양한 형식 지원 (HH:mm, H:mm 등)
            mealTime = LocalTime.parse(mealTimeStr, DateTimeFormatter.ofPattern("[HH:mm][H:mm][HH:m][H:m]"));
        } catch (Exception e) {
            log.error("식사 시간 파싱 실패: mealTimeStr={}, userId={}, mealType={}", 
                    mealTimeStr, user.getId(), mealType, e);
            throw new CustomException(ErrorCode.INVALID_MISSION_TYPE, 
                    String.format("%s 식사 시간 형식이 올바르지 않습니다.", mealType));
        }
        
        // 오늘 날짜의 식사 시간 생성
        LocalDateTime mealDateTime = LocalDate.now().atTime(mealTime);
        
        // 식사 시간 이전에는 인증 불가
        if (now.isBefore(mealDateTime)) {
            throw new CustomException(ErrorCode.SPONTANEOUS_MISSION_TIME_EXPIRED, 
                    String.format("%s 식사 시간 이전에는 인증할 수 없습니다.", mealType));
        }
        
        // 식사 시간 + 2시간이 마감 시간
        LocalDateTime deadline = mealDateTime.plusHours(2);
        
        // 현재 시간이 마감 시간을 초과했는지 확인
        if (now.isAfter(deadline)) {
            // 시간 초과 - 미션 실패 처리
            userMission.updateStatus(UserMissionStatus.FAILED);
            userMissionRepository.save(userMission);
            throw new CustomException(ErrorCode.SPONTANEOUS_MISSION_TIME_EXPIRED, 
                    String.format("%s 식사 미션 인증 시간(식사 시간 + 2시간)이 초과되었습니다.", mealType));
        }

        // 게시글 조회 및 소유자 확인
        Post post = postRepository.findByIdAndDelFlagFalse(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // 인증 성공 처리 (투두리스트 미션 완료 처리 포함)
        completeMissionVerification(userMission);

        // 경험치 지급 정보 (이미 completeMissionVerification에서 처리됨)
        int expReward = 10;

        // 인증 기록 생성 (게시글 연결)
        MissionVerification verification = MissionVerification.builder()
                .userMission(userMission)
                .post(post)
                .verifiedAt(now)
                .build();
        verificationRepository.save(verification);

        Duration elapsed = Duration.between(mealDateTime, now);
        log.info("식사 미션 인증 완료: userMissionId={}, userId={}, mealType={}, mealTime={}, elapsedMinutes={}, postId={}", 
                userMission.getId(), user.getId(), mealType, mealTimeStr, elapsed.toMinutes(), postId);

        // 돌발 미션은 배지 없음
        return buildVerifyResponse(userMission, verification, expReward, null);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
