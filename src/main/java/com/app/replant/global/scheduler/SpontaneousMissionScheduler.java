package com.app.replant.global.scheduler;

import com.app.replant.domain.meallog.entity.MealLog;
import com.app.replant.domain.meallog.enums.MealType;
import com.app.replant.domain.meallog.service.MealLogService;
import com.app.replant.domain.notification.entity.Notification;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.global.infrastructure.service.fcm.FcmService;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.spontaneousmission.entity.SpontaneousMission;
import com.app.replant.domain.spontaneousmission.enums.SpontaneousMissionType;
import com.app.replant.domain.spontaneousmission.service.SpontaneousMissionService;
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
    private final NotificationService notificationService;
    private final FcmService fcmService;
    private final MealLogService mealLogService;
    private final SpontaneousMissionService spontaneousMissionService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
            int expiredCount = spontaneousMissionService.processExpiredMissions();
            if (expiredCount > 0) {
                log.info("만료된 돌발 미션 {}개 처리 완료", expiredCount);
            }
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
                // DB에 "7:00"(H:mm)과 "07:00"(HH:mm) 형식이 혼재할 수 있으므로 둘 다 조회
                String targetTimeHH = targetTime; // "07:00" 형식
                String targetTimeH = currentTime.format(DateTimeFormatter.ofPattern("H:mm")); // "7:00" 형식
                
                log.info("조회 시간 형식: HH:mm={}, H:mm={}", targetTimeHH, targetTimeH);
                
                // 1. 기상 시간에 해당하는 사용자 조회 (두 가지 형식 모두)
                List<User> wakeUpUsers = new java.util.ArrayList<>(userRepository.findUsersByWakeTime(targetTimeHH));
                if (!targetTimeHH.equals(targetTimeH)) {
                    wakeUpUsers.addAll(userRepository.findUsersByWakeTime(targetTimeH));
                }
                log.info("기상 시간({}, {})에 해당하는 사용자 수: {}", targetTimeHH, targetTimeH, wakeUpUsers.size());
                
                // 각 사용자별 작업을 병렬로 처리 (TaskScheduler 스레드 풀 활용)
                wakeUpUsers.parallelStream().forEach(user -> {
                    processUserForTimeBasedMission(user, now, targetTime, 
                            user.getWakeTime(), 
                            () -> {
                                log.info("기상 시간 매칭! 사용자 {} 기상 미션 할당 시작 (wakeTime: {})", 
                                        user.getId(), user.getWakeTime());
                                assignWakeUpMission(user, now);
                            },
                            "기상",
                            assignedCount, skippedCount);
                });
                
                // 2. 식사 시간에 해당하는 사용자 조회 (두 가지 형식 모두)
                List<User> mealUsers = new java.util.ArrayList<>(userRepository.findUsersByMealTime(targetTimeHH));
                if (!targetTimeHH.equals(targetTimeH)) {
                    mealUsers.addAll(userRepository.findUsersByMealTime(targetTimeH));
                }
                log.info("식사 시간({}, {})에 해당하는 사용자 수: {}", targetTimeHH, targetTimeH, mealUsers.size());
                
                // 각 사용자별 작업을 병렬로 처리
                mealUsers.parallelStream().forEach(user -> {
                    processMealTimeUser(user, now, targetTime, assignedCount, skippedCount);
                });
                
                // 3. 취침 시간에 해당하는 사용자 조회 (두 가지 형식 모두)
                List<User> sleepUsers = new java.util.ArrayList<>(userRepository.findUsersBySleepTime(targetTimeHH));
                if (!targetTimeHH.equals(targetTimeH)) {
                    sleepUsers.addAll(userRepository.findUsersBySleepTime(targetTimeH));
                }
                log.info("취침 시간({}, {})에 해당하는 사용자 수: {}", targetTimeHH, targetTimeH, sleepUsers.size());
                
                // 각 사용자별 작업을 병렬로 처리
                sleepUsers.parallelStream().forEach(user -> {
                    processUserForTimeBasedMission(user, now, targetTime, 
                            user.getSleepTime(), 
                            () -> {
                                log.info("취침 시간 매칭! 사용자 {} 감성일기 미션 할당 (sleepTime: {})", 
                                        user.getId(), user.getSleepTime());
                                assignEmotionalDiaryMission(user, now);
                            },
                            "감성일기",
                            assignedCount, skippedCount);
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
     * 시간 기반 미션을 위한 사용자 처리 공통 로직
     * 설정일 체크, 시간 매칭, 미션 할당을 통합 처리
     */
    private void processUserForTimeBasedMission(
            User user, 
            LocalDateTime now, 
            String targetTime, 
            String userTime,
            Runnable missionAssigner,
            String missionType,
            AtomicInteger assignedCount,
            AtomicInteger skippedCount) {
        try {
            log.info("[DEBUG] 사용자 {} {} 미션 처리 시작 - userTime: {}, targetTime: {}", 
                    user.getId(), missionType, userTime, targetTime);
            
            // 설정한 날짜가 오늘이면 미션을 할당하지 않음 (악용 방지 - 다음날부터만 적용)
            if (shouldSkipUserForToday(user, now)) {
                log.info("[DEBUG] 사용자 {} - shouldSkipUserForToday 조건에 걸림", user.getId());
                skippedCount.incrementAndGet();
                return;
            }
            
            String roundedTime = roundTimeTo5Minutes(userTime);
            log.info("[DEBUG] 사용자 {} - roundedTime: {}, targetTime: {}, 매칭: {}", 
                    user.getId(), roundedTime, targetTime, targetTime.equals(roundedTime));
            
            if (roundedTime != null && targetTime.equals(roundedTime)) {
                missionAssigner.run();
                assignedCount.incrementAndGet();
            } else {
                log.info("[DEBUG] 사용자 {} - 시간 매칭 실패 (roundedTime={}, targetTime={})", 
                        user.getId(), roundedTime, targetTime);
            }
        } catch (Exception e) {
            log.error("사용자 {} {} 미션 할당 실패: {}", user.getId(), missionType, e.getMessage(), e);
            skippedCount.incrementAndGet();
        }
    }

    /**
     * 식사 시간 사용자 처리 (아침/점심/저녁 구분)
     */
    private void processMealTimeUser(
            User user,
            LocalDateTime now,
            String targetTime,
            AtomicInteger assignedCount,
            AtomicInteger skippedCount) {
        try {
            // 설정한 날짜가 오늘이면 미션을 할당하지 않음 (악용 방지 - 다음날부터만 적용)
            if (shouldSkipUserForToday(user, now)) {
                return;
            }
            
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
    }

    /**
     * 오늘 설정한 사용자는 다음날부터만 미션 할당 (악용 방지)
     * 돌발 미션 설정 시점(spontaneousMissionSetupAt)을 기준으로 체크
     * 
     * TODO: 프로덕션에서는 악용 방지를 위해 활성화 필요
     */
    private boolean shouldSkipUserForToday(User user, LocalDateTime now) {
        // 돌발 미션 설정 시점을 기준으로 체크 (updatedAt이 아닌 전용 필드 사용)
        LocalDateTime setupAt = user.getSpontaneousMissionSetupAt();
        LocalDate setupDate = setupAt != null ? setupAt.toLocalDate() : null;
        LocalDate today = now.toLocalDate();
        
        if (setupDate != null && setupDate.equals(today)) {
            // 테스트를 위해 로그만 남기고 스킵하지 않음 (프로덕션에서는 return true로 변경)
            log.info("사용자 {}는 오늘 돌발 미션 설정을 완료함 (설정일: {}, 오늘: {}) - 테스트 모드로 미션 할당 허용", 
                    user.getId(), setupDate, today);
            // return true;  // 테스트 중 비활성화
        }
        return false;
    }

    /**
     * 시간 문자열을 HH:mm 형식으로 정규화 (1분 단위 매칭)
     * 예: "7:00" -> "07:00", "9:30" -> "09:30", "12:51" -> "12:51"
     * DB에 "7:00" 형식과 "07:00" 형식이 혼재할 수 있으므로 정규화 필요
     */
    private String roundTimeTo5Minutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        
        try {
            // 다양한 형식 지원 (H:mm, HH:mm 등)
            LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("[HH:mm][H:mm][HH:m][H:m]"));
            // 항상 HH:mm 형식으로 정규화하여 반환
            return time.format(TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("시간 파싱 실패: {}", timeStr, e);
            return null;
        }
    }

    /**
     * 기상 미션 할당
     */
    private void assignWakeUpMission(User user, LocalDateTime now) {
        SpontaneousMission mission = spontaneousMissionService.assignMission(
                user, SpontaneousMissionType.WAKE_UP, now, null);
        
        if (mission != null) {
            log.info("기상 미션 할당 완료: userId={}, missionId={}", user.getId(), mission.getId());
            sendSpontaneousMissionNotification(user, "기상하기", "기상", mission.getId());
        } else {
            log.debug("사용자 {}는 오늘 이미 기상 미션이 할당됨", user.getId());
        }
    }

    /**
     * 식사 관련 미션 할당 (MealLog 테이블 사용)
     */
    private void assignMealMission(User user, LocalDateTime now, String mealType) {
        // MealType enum 변환
        MealType mealTypeEnum;
        try {
            mealTypeEnum = MealType.fromDisplayName(mealType);
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 식사 타입: {}", mealType);
            return;
        }

        // MealLogService를 통해 MealLog 할당
        MealLog mealLog = mealLogService.assignMealMission(user, mealTypeEnum, now.toLocalDate());
        
        if (mealLog == null) {
            log.debug("사용자 {}는 오늘 이미 {} 식사 MealLog가 할당됨", user.getId(), mealType);
            return;
        }

        // SpontaneousMissionType 변환
        SpontaneousMissionType missionType;
        switch (mealTypeEnum) {
            case BREAKFAST:
                missionType = SpontaneousMissionType.MEAL_BREAKFAST;
                break;
            case LUNCH:
                missionType = SpontaneousMissionType.MEAL_LUNCH;
                break;
            case DINNER:
                missionType = SpontaneousMissionType.MEAL_DINNER;
                break;
            default:
                log.warn("알 수 없는 식사 타입: {}", mealType);
                return;
        }

        // SpontaneousMission 할당
        SpontaneousMission mission = spontaneousMissionService.assignMission(
                user, missionType, now, mealLog);
        
        if (mission != null) {
            log.info("{} 식사 미션 할당 완료: userId={}, missionId={}, mealLogId={}", 
                    mealType, user.getId(), mission.getId(), mealLog.getId());
            
            // 알림 전송 (SSE/FCM) - missionId 전달
            sendMealMissionNotification(user, mealTypeEnum, mission.getId());
        } else {
            log.debug("사용자 {}는 오늘 이미 {} 식사 미션이 할당됨", user.getId(), mealType);
        }
    }

    /**
     * 식사 미션 알림 전송 (SpontaneousMission용)
     */
    private void sendMealMissionNotification(User user, MealType mealType, Long missionId) {
        String title = mealType.getDisplayName() + " 식사 시간입니다! 🍽️";
        String content = mealType.getDisplayName() + " 식사 미션이 도착했습니다. 게시글을 작성해주세요!";
        
        try {
            Notification savedNotification = notificationService.createAndPushNotification(
                    user,
                    NotificationType.SPONTANEOUS_MEAL,
                    title,
                    content,
                    "SPONTANEOUS_MISSION",  // 참조 타입 변경
                    missionId    // 참조 ID (spontaneousMissionId)
            );
            
            // FCM 추가 전송
            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                try {
                    boolean fcmSent = fcmService.sendNotificationWithRetry(user.getId(), savedNotification);
                    if (fcmSent) {
                        log.info("식사 미션 FCM 알림 전송 성공: userId={}, missionId={}", user.getId(), missionId);
                    }
                } catch (Exception e) {
                    log.warn("식사 미션 FCM 알림 전송 실패: userId={}, missionId={}, error={}", 
                            user.getId(), missionId, e.getMessage());
                }
            }
            
            log.info("식사 미션 알림 전송 완료: userId={}, mealType={}, missionId={}", 
                    user.getId(), mealType.getDisplayName(), missionId);
        } catch (Exception e) {
            log.error("식사 미션 알림 전송 실패: userId={}, mealType={}, missionId={}, error={}", 
                    user.getId(), mealType.getDisplayName(), missionId, e.getMessage(), e);
        }
    }

    /**
     * 감성일기 작성 미션 할당
     */
    private void assignEmotionalDiaryMission(User user, LocalDateTime now) {
        SpontaneousMission mission = spontaneousMissionService.assignMission(
                user, SpontaneousMissionType.DIARY, now, null);
        
        if (mission != null) {
            log.info("감성일기 미션 할당 완료: userId={}, missionId={}", user.getId(), mission.getId());
            sendSpontaneousMissionNotification(user, "감성일기 쓰기", "감성일기", mission.getId());
        } else {
            log.debug("사용자 {}는 오늘 이미 감성일기 미션이 할당됨", user.getId());
        }
    }


    /**
     * 돌발 미션 알림 전송 (SSE/FCM)
     * 프론트에서 알림을 받으면:
     * - 기상 미션: 인증 화면으로 이동 (인증하기 버튼)
     * - 식사 미션: 인증 화면으로 이동 (게시글 작성)
     * - 감성일기 미션: 감성일기 작성 화면으로 바로 이동
     */
    private void sendSpontaneousMissionNotification(User user, String missionTitle, String missionType, Long missionId) {
        if (missionId == null) {
            log.warn("missionId가 null이므로 알림을 전송하지 않습니다. userId={}, missionType={}", user.getId(), missionType);
            return;
        }
        
        try {
            log.info("돌발 미션 알림 전송 시작: userId={}, missionType={}, missionId={}, fcmToken={}", 
                    user.getId(), missionType, missionId, user.getFcmToken() != null ? "있음" : "없음");
            
            String title;
            String content;
            NotificationType notificationType;
            
            // 미션 타입에 따라 알림 내용과 타입 설정
            if ("기상".equals(missionType)) {
                title = "기상 시간입니다! 🌅";
                content = "기상 미션이 도착했습니다. 10분 안에 인증해주세요!";
                notificationType = NotificationType.SPONTANEOUS_WAKE_UP;  // 프론트에서 인증 화면으로 라우팅
            } else if (missionType.contains("식사")) {
                title = String.format("%s 시간입니다! 🍽️", missionType);
                content = String.format("%s 미션이 도착했습니다. 게시글을 작성해주세요!", missionType);
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
                    "SPONTANEOUS_MISSION",  // 참조 타입 변경
                    missionId    // 참조 ID (spontaneousMissionId)
            );
            
            // 기상 미션의 경우 중요한 알림이므로 FCM을 확실히 전송
            if ("기상".equals(missionType) && user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                try {
                    log.info("기상 미션 FCM 알림 추가 전송 시도: userId={}, missionId={}", user.getId(), missionId);
                    boolean fcmSent = fcmService.sendNotificationWithRetry(user.getId(), savedNotification);
                    if (fcmSent) {
                        log.info("기상 미션 FCM 알림 전송 성공: userId={}, missionId={}", user.getId(), missionId);
                    } else {
                        log.warn("기상 미션 FCM 알림 전송 실패: userId={}, missionId={}", user.getId(), missionId);
                    }
                } catch (Exception e) {
                    log.error("기상 미션 FCM 알림 전송 중 예외 발생: userId={}, missionId={}, error={}", 
                            user.getId(), missionId, e.getMessage(), e);
                }
            }
            
            log.info("돌발 미션 알림 전송 완료: userId={}, missionType={}, missionId={}, notificationType={}", 
                    user.getId(), missionType, missionId, notificationType);
        } catch (Exception e) {
            log.error("돌발 미션 알림 전송 실패: userId={}, missionType={}, missionId={}, error={}", 
                    user.getId(), missionType, missionId, e.getMessage(), e);
            e.printStackTrace();
            // 알림 전송 실패해도 미션 할당은 성공했으므로 계속 진행
        }
    }

}
