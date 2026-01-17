package com.app.replant.scheduler;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionCategory;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.notification.entity.Notification;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.service.fcm.FcmService;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 돌발 미션 스케줄러
 * 사용자가 설정한 시간(기상, 식사 등)에 맞춰 돌발 미션을 자동 할당합니다.
 * 
 * 실행 주기: 매 1분마다 (정확한 시간 매칭을 위해)
 * 
 * 할당되는 미션 종류:
 * - 기상 시간: 기상 미션
 * - 아침 식사 시간: 아침 식사 관련 미션
 * - 점심 식사 시간: 점심 식사 관련 미션
 * - 저녁 식사 시간: 저녁 식사 관련 미션
 * - 취침 시간: 감성일기 작성 미션
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpontaneousMissionScheduler {

    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final NotificationService notificationService;
    private final FcmService fcmService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    // 돌발 미션 캐시 (성능 최적화)
    private volatile Mission cachedWakeUpMission = null;
    private volatile Mission cachedMealMission = null;
    private volatile Mission cachedDiaryMission = null;
    private volatile LocalDateTime lastCacheUpdate = null;
    private static final long CACHE_TTL_MINUTES = 60; // 캐시 유효 시간: 1시간

    /**
     * 매 1분마다 실행 (더 정확한 시간 매칭을 위해)
     * cron: "초 분 시 일 월 요일"
     * zone = "Asia/Seoul"이므로 KST 기준으로 실행
     * 
     * TaskScheduler 설정으로 효율적인 리소스 관리 및 동시 작업 처리
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void assignSpontaneousMissions() {
        try {
            log.info("=== 스케줄러 실행 확인: {} ===", LocalDateTime.now());
            assignSpontaneousMissionsInternal();
        } catch (Exception e) {
            log.error("돌발 미션 할당 스케줄러 실행 중 예외 발생", e);
            e.printStackTrace();
        }
        
        try {
            processExpiredSpontaneousMissions();
        } catch (Exception e) {
            log.error("돌발 미션 시간 초과 처리 중 예외 발생", e);
            e.printStackTrace();
        }
    }

    /**
     * 돌발 미션 할당
     */
    private void assignSpontaneousMissionsInternal() {
        log.info("=== 돌발 미션 할당 스케줄러 시작 ===");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();
            
            // 현재 시간을 그대로 사용 (1분 단위로 실행하므로 정확한 매칭 가능)
            String targetTime = currentTime.format(TIME_FORMATTER);
            
            log.info("현재 시간: {}, 타겟 시간: {}", currentTime, targetTime);
            
            AtomicInteger assignedCount = new AtomicInteger(0);
            AtomicInteger skippedCount = new AtomicInteger(0);
            
            try {
                // 1. 기상 시간에 해당하는 사용자만 조회
                List<User> wakeUpUsers = userRepository.findUsersByWakeTime(targetTime);
                log.info("기상 시간({})에 해당하는 사용자 수: {}", targetTime, wakeUpUsers.size());
                
                // 각 사용자별 작업을 병렬로 처리 (TaskScheduler 스레드 풀 활용)
                wakeUpUsers.parallelStream().forEach(user -> {
                    try {
                        String roundedWakeTime = roundTimeTo5Minutes(user.getWakeTime());
                        if (roundedWakeTime != null && targetTime.equals(roundedWakeTime)) {
                            log.info("기상 시간 매칭! 사용자 {} 기상 미션 할당 시작 (wakeTime: {})", 
                                    user.getId(), user.getWakeTime());
                            assignWakeUpMission(user, now);
                            assignedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("사용자 {} 기상 미션 할당 실패: {}", user.getId(), e.getMessage(), e);
                        skippedCount.incrementAndGet();
                    }
                });
                
                // 2. 식사 시간에 해당하는 사용자 조회 (아침/점심/저녁)
                List<User> mealUsers = userRepository.findUsersByMealTime(targetTime);
                log.info("식사 시간({})에 해당하는 사용자 수: {}", targetTime, mealUsers.size());
                
                // 각 사용자별 작업을 병렬로 처리
                mealUsers.parallelStream().forEach(user -> {
                    try {
                        String roundedBreakfastTime = roundTimeTo5Minutes(user.getBreakfastTime());
                        String roundedLunchTime = roundTimeTo5Minutes(user.getLunchTime());
                        String roundedDinnerTime = roundTimeTo5Minutes(user.getDinnerTime());
                        
                        if (roundedBreakfastTime != null && targetTime.equals(roundedBreakfastTime)) {
                            log.info("아침 식사 시간 매칭! 사용자 {} 아침 식사 미션 할당 (breakfastTime: {})", 
                                    user.getId(), user.getBreakfastTime());
                            assignMealMission(user, now, "아침");
                            assignedCount.incrementAndGet();
                        } else if (roundedLunchTime != null && targetTime.equals(roundedLunchTime)) {
                            log.info("점심 식사 시간 매칭! 사용자 {} 점심 식사 미션 할당 (lunchTime: {})", 
                                    user.getId(), user.getLunchTime());
                            assignMealMission(user, now, "점심");
                            assignedCount.incrementAndGet();
                        } else if (roundedDinnerTime != null && targetTime.equals(roundedDinnerTime)) {
                            log.info("저녁 식사 시간 매칭! 사용자 {} 저녁 식사 미션 할당 (dinnerTime: {})", 
                                    user.getId(), user.getDinnerTime());
                            assignMealMission(user, now, "저녁");
                            assignedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("사용자 {} 식사 미션 할당 실패: {}", user.getId(), e.getMessage(), e);
                        skippedCount.incrementAndGet();
                    }
                });
                
                // 3. 취침 시간에 해당하는 사용자만 조회
                List<User> sleepUsers = userRepository.findUsersBySleepTime(targetTime);
                log.info("취침 시간({})에 해당하는 사용자 수: {}", targetTime, sleepUsers.size());
                
                // 각 사용자별 작업을 병렬로 처리
                sleepUsers.parallelStream().forEach(user -> {
                    try {
                        String roundedSleepTime = roundTimeTo5Minutes(user.getSleepTime());
                        if (roundedSleepTime != null && targetTime.equals(roundedSleepTime)) {
                            log.info("취침 시간 매칭! 사용자 {} 감성일기 미션 할당 (sleepTime: {})", 
                                    user.getId(), user.getSleepTime());
                            assignEmotionalDiaryMission(user, now);
                            assignedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("사용자 {} 감성일기 미션 할당 실패: {}", user.getId(), e.getMessage(), e);
                        skippedCount.incrementAndGet();
                    }
                });
                
            } catch (Exception e) {
                log.error("사용자 조회 실패", e);
                e.printStackTrace();
                return;
            }
            
            log.info("=== 돌발 미션 할당 스케줄러 완료 === 할당: {}, 스킵: {}", assignedCount.get(), skippedCount.get());
            
        } catch (Exception e) {
            log.error("돌발 미션 할당 스케줄러 실행 중 오류 발생", e);
        }
    }

    /**
     * 시간 문자열을 그대로 반환 (1분 단위 매칭)
     * 예: "12:51" -> "12:51", "12:07" -> "12:07"
     */
    private String roundTimeTo5Minutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        
        try {
            // 시간 형식 검증만 수행
            LocalTime.parse(timeStr, TIME_FORMATTER);
            return timeStr;  // 그대로 반환
        } catch (Exception e) {
            log.warn("시간 파싱 실패: {}", timeStr, e);
            return null;
        }
    }

    /**
     * 기상 미션 할당
     */
    private void assignWakeUpMission(User user, LocalDateTime now) {
        // 오늘 이미 기상 미션이 할당되었는지 확인
        if (hasSpontaneousMissionToday(user, "기상", now.toLocalDate())) {
            log.debug("사용자 {}는 오늘 이미 기상 미션이 할당됨", user.getId());
            return;
        }
        
        // 기상 관련 미션 찾기 (자기관리 카테고리, 난이도 낮음)
        Optional<Mission> wakeUpMission = missionRepository.findAll().stream()
                .filter(mission -> mission.getMissionType() == MissionType.OFFICIAL)
                .filter(mission -> Boolean.TRUE.equals(mission.getIsActive()))
                .filter(mission -> mission.getCategory() == MissionCategory.DAILY_LIFE 
                        || mission.getCategory() == MissionCategory.HEALTH)
                .filter(mission -> mission.getTitle().contains("기상") || mission.getTitle().contains("일어나"))
                .findFirst();
        
        if (wakeUpMission.isEmpty()) {
            log.warn("기상 미션을 찾을 수 없습니다. 기본 미션을 할당합니다.");
            // 기본 미션 찾기
            wakeUpMission = missionRepository.findAll().stream()
                    .filter(mission -> mission.getMissionType() == MissionType.OFFICIAL)
                    .filter(mission -> Boolean.TRUE.equals(mission.getIsActive()))
                    .filter(mission -> mission.getCategory() == MissionCategory.DAILY_LIFE)
                    .findFirst();
        }
        
        if (wakeUpMission.isPresent()) {
            UserMission userMission = assignMissionToUser(user, wakeUpMission.get(), now, "기상");
            if (userMission != null) {
                log.info("기상 미션 할당 완료: userId={}, missionId={}, userMissionId={}", user.getId(), wakeUpMission.get().getId(), userMission.getId());
                
                // 알림 전송 (SSE/FCM)
                sendSpontaneousMissionNotification(user, wakeUpMission.get().getTitle(), "기상", userMission.getId());
            } else {
                log.warn("기상 미션 할당 실패: userMission이 null입니다. (이미 할당되었거나 중복일 수 있음)");
            }
        } else {
            log.warn("할당할 기상 미션이 없습니다.");
        }
    }

    /**
     * 식사 관련 미션 할당
     */
    private void assignMealMission(User user, LocalDateTime now, String mealType) {
        // 오늘 이미 해당 식사 미션이 할당되었는지 확인
        if (hasSpontaneousMissionToday(user, mealType, now.toLocalDate())) {
            log.debug("사용자 {}는 오늘 이미 {} 식사 미션이 할당됨", user.getId(), mealType);
            return;
        }
        
        // 식사 관련 미션 찾기
        Optional<Mission> mealMission = missionRepository.findAll().stream()
                .filter(mission -> mission.getMissionType() == MissionType.OFFICIAL)
                .filter(mission -> Boolean.TRUE.equals(mission.getIsActive()))
                .filter(mission -> mission.getCategory() == MissionCategory.HEALTH 
                        || mission.getCategory() == MissionCategory.DAILY_LIFE)
                .filter(mission -> mission.getTitle().contains("식사") 
                        || mission.getTitle().contains("아침") 
                        || mission.getTitle().contains("점심")
                        || mission.getTitle().contains("저녁")
                        || mission.getTitle().contains("밥"))
                .findFirst();
        
        if (mealMission.isEmpty()) {
            log.warn("{} 식사 미션을 찾을 수 없습니다. 기본 미션을 할당합니다.", mealType);
            mealMission = missionRepository.findAll().stream()
                    .filter(mission -> mission.getMissionType() == MissionType.OFFICIAL)
                    .filter(mission -> Boolean.TRUE.equals(mission.getIsActive()))
                    .filter(mission -> mission.getCategory() == MissionCategory.HEALTH)
                    .findFirst();
        }
        
        if (mealMission.isPresent()) {
            UserMission userMission = assignMissionToUser(user, mealMission.get(), now, mealType);
            log.info("{} 식사 미션 할당: userId={}, missionId={}", mealType, user.getId(), mealMission.get().getId());
            
            // 알림 전송 (SSE/FCM)
            sendSpontaneousMissionNotification(user, mealMission.get().getTitle(), mealType + " 식사", userMission.getId());
        } else {
            log.warn("할당할 {} 식사 미션이 없습니다.", mealType);
        }
    }

    /**
     * 감성일기 작성 미션 할당
     */
    private void assignEmotionalDiaryMission(User user, LocalDateTime now) {
        // 오늘 이미 감성일기 미션이 할당되었는지 확인
        if (hasSpontaneousMissionToday(user, "감성일기", now.toLocalDate())) {
            log.debug("사용자 {}는 오늘 이미 감성일기 미션이 할당됨", user.getId());
            return;
        }
        
        // 감성일기 관련 미션 찾기
        Optional<Mission> diaryMission = missionRepository.findAll().stream()
                .filter(mission -> mission.getMissionType() == MissionType.OFFICIAL)
                .filter(mission -> Boolean.TRUE.equals(mission.getIsActive()))
                .filter(mission -> mission.getCategory() == MissionCategory.GROWTH 
                        || mission.getCategory() == MissionCategory.DAILY_LIFE)
                .filter(mission -> mission.getTitle().contains("일기") 
                        || mission.getTitle().contains("감성")
                        || mission.getTitle().contains("글쓰기")
                        || mission.getTitle().contains("기록"))
                .findFirst();
        
        if (diaryMission.isEmpty()) {
            log.warn("감성일기 미션을 찾을 수 없습니다. 기본 미션을 할당합니다.");
            diaryMission = missionRepository.findAll().stream()
                    .filter(mission -> mission.getMissionType() == MissionType.OFFICIAL)
                    .filter(mission -> Boolean.TRUE.equals(mission.getIsActive()))
                    .filter(mission -> mission.getCategory() == MissionCategory.GROWTH)
                    .findFirst();
        }
        
        if (diaryMission.isPresent()) {
            UserMission userMission = assignMissionToUser(user, diaryMission.get(), now, "감성일기");
            log.info("감성일기 미션 할당: userId={}, missionId={}", user.getId(), diaryMission.get().getId());
            
            // 알림 전송 (SSE/FCM)
            sendSpontaneousMissionNotification(user, diaryMission.get().getTitle(), "감성일기", userMission.getId());
        } else {
            log.warn("할당할 감성일기 미션이 없습니다.");
        }
    }

    /**
     * 사용자에게 미션 할당
     * @return 할당된 UserMission 엔티티
     */
    private UserMission assignMissionToUser(User user, Mission mission, LocalDateTime now, String missionType) {
        // 이미 할당된 미션인지 확인 (중복 방지)
        boolean alreadyAssigned = userMissionRepository.findByUserIdWithFilters(
                user.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 1)
        ).stream()
                .anyMatch(um -> um.getMission() != null 
                        && um.getMission().getId().equals(mission.getId())
                        && (um.getStatus() == UserMissionStatus.ASSIGNED 
                                || um.getStatus() == UserMissionStatus.PENDING)
                        && um.getAssignedAt().toLocalDate().equals(now.toLocalDate()));
        
        if (alreadyAssigned) {
            log.debug("사용자 {}는 이미 미션 {}가 할당되어 있음", user.getId(), mission.getId());
            return null;
        }
        
        // 미션 기간 설정 (돌발 미션은 당일 종료로 설정)
        LocalDateTime dueDate = now.toLocalDate().atTime(23, 59, 59);
        
        UserMission userMission = UserMission.builder()
                .user(user)
                .mission(mission)
                .missionType(MissionType.OFFICIAL)
                .assignedAt(now)
                .dueDate(dueDate)
                .status(UserMissionStatus.ASSIGNED)
                .isSpontaneous(true)  // 돌발 미션으로 표시
                .build();
        
        UserMission saved = userMissionRepository.save(userMission);
        log.info("돌발 미션 할당 완료: userId={}, missionId={}, type={}", 
                user.getId(), mission.getId(), missionType);
        
        return saved;
    }

    /**
     * 돌발 미션 알림 전송 (SSE/FCM)
     * 프론트에서 알림을 받으면:
     * - 기상 미션: 인증 화면으로 이동 (인증하기 버튼)
     * - 식사 미션: 인증 화면으로 이동 (게시글 작성)
     * - 감성일기 미션: 감성일기 작성 화면으로 바로 이동
     */
    private void sendSpontaneousMissionNotification(User user, String missionTitle, String missionType, Long userMissionId) {
        if (userMissionId == null) {
            log.warn("userMissionId가 null이므로 알림을 전송하지 않습니다. userId={}, missionType={}", user.getId(), missionType);
            return;
        }
        
        try {
            log.info("돌발 미션 알림 전송 시작: userId={}, missionType={}, userMissionId={}, fcmToken={}", 
                    user.getId(), missionType, userMissionId, user.getFcmToken() != null ? "있음" : "없음");
            
            String title;
            String content;
            NotificationType notificationType;
            
            // 미션 타입에 따라 알림 내용과 타입 설정
            if ("기상".equals(missionType)) {
                title = "기상 시간입니다! 🌅";
                content = "기상 미션이 도착했습니다. 10분 안에 인증해주세요!";
                notificationType = NotificationType.SPONTANEOUS_WAKE_UP;  // 프론트에서 인증 화면으로 라우팅
            } else if (missionType.contains("식사")) {
                title = String.format("%s 식사 시간입니다! 🍽️", missionType);
                content = String.format("%s 식사 미션이 도착했습니다. 식사 후 게시글을 작성해주세요!", missionType);
                notificationType = NotificationType.SPONTANEOUS_MEAL;  // 프론트에서 인증 화면으로 라우팅
            } else if ("감성일기".equals(missionType)) {
                title = "감성일기 작성 시간입니다! ✍️";
                content = "오늘 하루를 돌아보며 감성일기를 작성해보세요.";
                notificationType = NotificationType.SPONTANEOUS_DIARY;  // 프론트에서 감성일기 작성 화면으로 바로 이동
            } else {
                title = "돌발 미션이 도착했습니다! 🎯";
                content = String.format("%s 시간입니다. '%s' 미션을 확인해보세요!", missionType, missionTitle);
                notificationType = NotificationType.MISSION_ASSIGNED;
            }
            
            Notification savedNotification = notificationService.createAndPushNotification(
                    user,
                    notificationType,
                    title,
                    content,
                    "USER_MISSION",  // 참조 타입
                    userMissionId    // 참조 ID (userMissionId)
            );
            
            // 기상 미션의 경우 중요한 알림이므로 FCM을 확실히 전송
            if ("기상".equals(missionType) && user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                try {
                    log.info("기상 미션 FCM 알림 추가 전송 시도: userId={}, userMissionId={}", user.getId(), userMissionId);
                    boolean fcmSent = fcmService.sendNotificationWithRetry(user.getId(), savedNotification);
                    if (fcmSent) {
                        log.info("기상 미션 FCM 알림 전송 성공: userId={}, userMissionId={}", user.getId(), userMissionId);
                    } else {
                        log.warn("기상 미션 FCM 알림 전송 실패: userId={}, userMissionId={}", user.getId(), userMissionId);
                    }
                } catch (Exception e) {
                    log.error("기상 미션 FCM 알림 전송 중 예외 발생: userId={}, userMissionId={}, error={}", 
                            user.getId(), userMissionId, e.getMessage(), e);
                }
            }
            
            log.info("돌발 미션 알림 전송 완료: userId={}, missionType={}, userMissionId={}, notificationType={}", 
                    user.getId(), missionType, userMissionId, notificationType);
        } catch (Exception e) {
            log.error("돌발 미션 알림 전송 실패: userId={}, missionType={}, userMissionId={}, error={}", 
                    user.getId(), missionType, userMissionId, e.getMessage(), e);
            e.printStackTrace();
            // 알림 전송 실패해도 미션 할당은 성공했으므로 계속 진행
        }
    }

    /**
     * 오늘 이미 해당 유형의 돌발 미션이 할당되었는지 확인
     * 미션 타입별로 구분해서 체크 (기상, 식사, 일기는 각각 별도로 할당 가능)
     */
    private boolean hasSpontaneousMissionToday(User user, String missionType, LocalDate today) {
        List<UserMission> todayMissions = userMissionRepository.findByUserIdWithFilters(
                user.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 100)
        ).stream()
                .filter(um -> um.isSpontaneousMission())
                .filter(um -> um.getAssignedAt().toLocalDate().equals(today))
                .filter(um -> um.getStatus() == UserMissionStatus.ASSIGNED 
                        || um.getStatus() == UserMissionStatus.PENDING)
                .toList();
        
        if (todayMissions.isEmpty()) {
            return false;
        }
        
        // 미션 타입별로 구분해서 체크
        for (UserMission um : todayMissions) {
            if (um.getMission() == null) continue;
            
            String missionTitle = um.getMission().getTitle();
            
            // 기상 미션 체크
            if ("기상".equals(missionType)) {
                if (missionTitle.contains("기상") || missionTitle.contains("일어나")) {
                    log.debug("사용자 {}는 오늘 이미 기상 미션이 할당됨: missionId={}", user.getId(), um.getMission().getId());
                    return true;
                }
            }
            
            // 식사 미션 체크 (아침/점심/저녁 구분)
            if (missionType.contains("식사")) {
                if (missionTitle.contains("식사") || missionTitle.contains("밥")) {
                    // 아침/점심/저녁 구분
                    if (missionType.contains("아침") && missionTitle.contains("아침")) {
                        log.debug("사용자 {}는 오늘 이미 아침 식사 미션이 할당됨: missionId={}", user.getId(), um.getMission().getId());
                        return true;
                    } else if (missionType.contains("점심") && missionTitle.contains("점심")) {
                        log.debug("사용자 {}는 오늘 이미 점심 식사 미션이 할당됨: missionId={}", user.getId(), um.getMission().getId());
                        return true;
                    } else if (missionType.contains("저녁") && missionTitle.contains("저녁")) {
                        log.debug("사용자 {}는 오늘 이미 저녁 식사 미션이 할당됨: missionId={}", user.getId(), um.getMission().getId());
                        return true;
                    }
                }
            }
            
            // 감성일기 미션 체크
            if ("감성일기".equals(missionType)) {
                if (missionTitle.contains("일기") || missionTitle.contains("감성")) {
                    log.debug("사용자 {}는 오늘 이미 감성일기 미션이 할당됨: missionId={}", user.getId(), um.getMission().getId());
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 기상 미션 시간 초과 처리 (비활성화됨)
     * 사용자가 인증을 시도할 때만 10분 초과 여부를 확인하도록 변경
     * 스케줄러에서는 자동으로 실패 처리하지 않음
     */
    private void processExpiredSpontaneousMissions() {
        // 비활성화: 사용자가 인증을 시도할 때만 10분 초과 여부를 확인
        // 스케줄러에서는 자동으로 실패 처리하지 않음
        // log.info("=== 돌발 미션 시간 초과 처리 시작 ===");
        // 기상 미션은 사용자가 인증을 시도할 때만 시간 초과 체크 (UserMissionService.verifyWakeUpMission)
    }
}
