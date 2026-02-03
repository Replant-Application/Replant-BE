package com.app.replant.global.scheduler;

import com.app.replant.domain.meallog.entity.MealLog;
import com.app.replant.domain.meallog.enums.MealType;
import com.app.replant.domain.meallog.service.MealLogService;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionCategory;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.spontaneousmission.entity.SpontaneousMission;
import com.app.replant.domain.spontaneousmission.enums.SpontaneousMissionType;
import com.app.replant.domain.spontaneousmission.repository.SpontaneousMissionRepository;
import com.app.replant.domain.notification.entity.Notification;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.global.infrastructure.service.fcm.FcmService;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * - 취침 시간: 감정일기 작성 미션
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
    private final MealLogService mealLogService;
    private final SpontaneousMissionRepository spontaneousMissionRepository;

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
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    /**
     * [테스트용] 설정 저장/수정 시 즉시 기상 미션 할당 + 알림 전송.
     * 스케줄 시간까지 기다리지 않고, 호출 시점에 바로 할당·알림.
     */
    @Transactional
    public void assignWakeUpMissionAndNotifyImmediately(User user) {
        LocalDateTime now = ZonedDateTime.now(ZONE_SEOUL).toLocalDateTime();
        log.info("[기상미션] 테스트: 설정 저장/수정 즉시 할당·알림 userId={}, now(KST)={}", user.getId(), now);
        assignWakeUpMission(user, now);
    }

    private void assignSpontaneousMissionsInternal() {
        log.info("=== 돌발 미션 할당 스케줄러 시작 ===");
        
        try {
            // KST 기준 현재 시각 사용 (서버 JVM이 UTC여도 사용자 기상/식사 시간과 일치)
            ZonedDateTime nowKst = ZonedDateTime.now(ZONE_SEOUL);
            LocalDateTime now = nowKst.toLocalDateTime();
            LocalTime currentTime = now.toLocalTime();
            
            String targetTime = currentTime.format(TIME_FORMATTER);
            
            log.info("현재 시간(KST): {}, 타겟 시간: {}", currentTime, targetTime);
            
            AtomicInteger assignedCount = new AtomicInteger(0);
            AtomicInteger skippedCount = new AtomicInteger(0);
            
            try {
                // DB에 "7:00"(H:mm)과 "07:00"(HH:mm) 형식이 혼재할 수 있으므로 둘 다 조회
                String targetTimeHH = targetTime; // "07:00" 형식
                String targetTimeH = currentTime.format(DateTimeFormatter.ofPattern("H:mm")); // "7:00" 형식
                
                log.info("[기상미션] 스케줄러 분 실행 KST now={}, targetTime(HH:mm)={}, targetTime(H:mm)={}", 
                    now, targetTimeHH, targetTimeH);
                
                List<User> wakeUpUsers = new java.util.ArrayList<>(userRepository.findUsersByWakeTime(targetTimeHH));
                if (!targetTimeHH.equals(targetTimeH)) {
                    wakeUpUsers.addAll(userRepository.findUsersByWakeTime(targetTimeH));
                }
                log.info("[기상미션] 기상 시간 매칭 사용자 수={}, userIds={}", 
                        wakeUpUsers.size(), wakeUpUsers.stream().map(User::getId).toList());
                
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
                                log.info("취침 시간 매칭! 사용자 {} 감정일기 미션 할당 (sleepTime: {})", 
                                        user.getId(), user.getSleepTime());
                                assignEmotionalDiaryMission(user, now);
                            },
                            "감정일기",
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
            log.info("[기상미션] 사용자 {} {} 처리 userTime={}, targetTime={}", 
                    user.getId(), missionType, userTime, targetTime);
            
            if (shouldSkipUserForToday(user, now)) {
                log.info("[기상미션] 사용자 {} 스킵(오늘 설정 완료)", user.getId());
                skippedCount.incrementAndGet();
                return;
            }
            
            String roundedTime = roundTimeTo5Minutes(userTime);
            boolean matched = roundedTime != null && targetTime.equals(roundedTime);
            log.info("[기상미션] 사용자 {} roundedTime={}, targetTime={}, 매칭={}", 
                    user.getId(), roundedTime, targetTime, matched);
            
            if (matched) {
                missionAssigner.run();
                assignedCount.incrementAndGet();
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
        log.info("[기상미션] 할당 시도 userId={}, userWakeTime={}, 스케줄러now(KST)={}", 
                user.getId(), user.getWakeTime(), now);
        
        // [테스트용 비활성화] 오늘 이미 기상 미션이 할당되었는지 확인 - 테스트 후 복구
        // if (hasSpontaneousMissionToday(user, "기상", now.toLocalDate())) {
        //     log.debug("사용자 {}는 오늘 이미 기상 미션이 할당됨", user.getId());
        //     return;
        // }
        
        Optional<SpontaneousMission> spontaneousMissionOpt = spontaneousMissionRepository
                .findByMissionType(SpontaneousMissionType.WAKE_UP);
        
        UserMission userMission;
        String titleForNotification;
        if (spontaneousMissionOpt.isPresent()) {
            SpontaneousMission spontaneousMission = spontaneousMissionOpt.get();
            titleForNotification = spontaneousMission.getTitle();
            log.info("[기상미션] spontaneous_mission 조회됨 id={}, title={}", spontaneousMission.getId(), titleForNotification);
            userMission = assignSpontaneousMissionToUser(user, spontaneousMission, now, "기상");
        } else {
            // [폴백] spontaneous_mission에 WAKE_UP 없어도 기상 미션 할당 + 알림 (테스트/운영 안정성)
            log.warn("[기상미션] spontaneous_mission에 WAKE_UP 없음 → 폴백으로 UserMission 생성 후 알림 전송 userId={}", user.getId());
            userMission = assignWakeUpMissionFallback(user, now);
            titleForNotification = "기상하기";
        }
        if (userMission != null) {
            log.info("[기상미션] 할당 완료 userId={}, userMissionId={}, assignedAt={}, 알림 전송 예정", 
                    user.getId(), userMission.getId(), userMission.getAssignedAt());
            sendSpontaneousMissionNotification(user, titleForNotification, "기상", userMission.getId());
        } else {
            log.warn("[기상미션] 할당 실패 userMission=null userId={}", user.getId());
        }
    }

    /**
     * [폴백] spontaneous_mission에 WAKE_UP 없을 때 기상 미션만 할당 (mission=null)
     * 테스트용: due_date는 DB NOT NULL 대응용만 사용, 만료 판정에 미사용
     */
    private UserMission assignWakeUpMissionFallback(User user, LocalDateTime now) {
        LocalDateTime dueDate = now.plusDays(1); // 테스트용, 미사용
        UserMission userMission = UserMission.builder()
                .user(user)
                .mission(null)
                .missionType(MissionType.OFFICIAL)
                .assignedAt(now)
                .dueDate(dueDate)
                .status(UserMissionStatus.ASSIGNED)
                .isSpontaneous(true)
                .build();
        return userMissionRepository.save(userMission);
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

        // MealLogService를 통해 미션 할당 (기존 미션이 있으면 그것을 반환)
        MealLog mealLog = mealLogService.assignMealMission(user, mealTypeEnum, now.toLocalDate());
        
        if (mealLog != null) {
            // 미션이 새로 할당되었거나 기존 미션이 있는 경우
            // ASSIGNED 상태이고 만료되지 않은 경우에만 알림 전송
            if (mealLog.getStatus() == com.app.replant.domain.meallog.enums.MealLogStatus.ASSIGNED 
                    && !mealLog.isExpired()) {
                log.info("{} 식사 미션 알림 전송: userId={}, mealLogId={}, status={}, assignedAt={}, deadlineAt={}", 
                        mealType, user.getId(), mealLog.getId(), mealLog.getStatus(),
                        mealLog.getAssignedAt(), mealLog.getDeadlineAt());
                
                // 알림 전송 (SSE/FCM) - mealLogId 전달
                sendMealMissionNotification(user, mealTypeEnum, mealLog.getId());
            } else {
                log.info("{} 식사 미션은 상태가 {}이거나 만료되어 알림을 전송하지 않음: userId={}, mealLogId={}, status={}, expired={}", 
                        mealType, mealLog.getStatus(), user.getId(), mealLog.getId(), 
                        mealLog.getStatus(), mealLog.isExpired());
            }
        } else {
            log.warn("식사 미션 할당 실패: userId={}, mealType={}", user.getId(), mealType);
        }
    }

    /**
     * 식사 미션 알림 전송 (MealLog용)
     */
    private void sendMealMissionNotification(User user, MealType mealType, Long mealLogId) {
        String title = mealType.getDisplayName() + " 식사 시간입니다! 🍽️";
        String content = mealType.getDisplayName() + " 식사 미션이 도착했습니다. 게시글을 작성해주세요!";
        
        try {
            Notification savedNotification = notificationService.createAndPushNotification(
                    user,
                    NotificationType.SPONTANEOUS_MEAL,
                    title,
                    content,
                    "MEAL_LOG",  // 참조 타입
                    mealLogId    // 참조 ID (mealLogId)
            );
            
            // FCM 추가 전송
            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                try {
                    boolean fcmSent = fcmService.sendNotificationWithRetry(user.getId(), savedNotification);
                    if (fcmSent) {
                        log.info("식사 미션 FCM 알림 전송 성공: userId={}, mealLogId={}", user.getId(), mealLogId);
                    }
                } catch (Exception e) {
                    log.warn("식사 미션 FCM 알림 전송 실패: userId={}, mealLogId={}, error={}", 
                            user.getId(), mealLogId, e.getMessage());
                }
            }
            
            log.info("식사 미션 알림 전송 완료: userId={}, mealType={}, mealLogId={}", 
                    user.getId(), mealType.getDisplayName(), mealLogId);
        } catch (Exception e) {
            log.error("식사 미션 알림 전송 실패: userId={}, mealType={}, mealLogId={}, error={}", 
                    user.getId(), mealType.getDisplayName(), mealLogId, e.getMessage(), e);
        }
    }

    /**
     * 감정일기 작성 알림 전송 (미션 할당 없이 알림만 전송)
     */
    private void assignEmotionalDiaryMission(User user, LocalDateTime now) {
        // spontaneous_mission 테이블에서 감정일기 미션 정보 조회
        Optional<SpontaneousMission> spontaneousMissionOpt = spontaneousMissionRepository
                .findByMissionType(SpontaneousMissionType.DIARY);
        
        if (spontaneousMissionOpt.isEmpty()) {
            log.warn("spontaneous_mission 테이블에서 감정일기 미션을 찾을 수 없습니다. userId={}", user.getId());
            return;
        }
        
        SpontaneousMission spontaneousMission = spontaneousMissionOpt.get();
        String spontaneousTitle = spontaneousMission.getTitle();
        
        log.debug("spontaneous_mission에서 조회한 감정일기 미션: title={}, description={}, missionType={}", 
                spontaneousTitle, spontaneousMission.getDescription(), spontaneousMission.getMissionType());
        
        // 감정일기는 공식 미션이 아니므로 UserMission 생성 없이 알림만 전송
        // userMissionId는 null로 전달 (알림에서 처리)
        sendSpontaneousMissionNotification(user, spontaneousTitle, "감정일기", null);
        
        log.info("감정일기 알림 전송 완료: userId={}, spontaneousMissionId={}, title={}", 
                user.getId(), spontaneousMission.getId(), spontaneousTitle);
    }

    /**
     * 사용자에게 돌발 미션 할당 (spontaneous_mission 테이블 사용)
     * @return 할당된 UserMission 엔티티
     */
    private UserMission assignSpontaneousMissionToUser(User user, SpontaneousMission spontaneousMission, 
                                                       LocalDateTime now, String missionType) {
        // 중복 체크는 호출하는 쪽(assignWakeUpMission 등)에서 이미 수행하므로 여기서는 생략
        // assignWakeUpMission에서 hasSpontaneousMissionToday를 호출하여 타입별로 구분해서 체크함
        
        // 기상 미션: 테스트용이라 due_date 미사용 (DB NOT NULL 대응만). 그 외 돌발 미션은 당일 23:59
        LocalDateTime dueDate = "기상".equals(missionType)
                ? now.plusDays(1)
                : now.toLocalDate().atTime(23, 59, 59);
        
        // 돌발 미션은 mission을 null로 설정 (spontaneous_mission 테이블에만 존재)
        UserMission userMission = UserMission.builder()
                .user(user)
                .mission(null)  // 돌발 미션은 mission 테이블에 없음
                .missionType(MissionType.OFFICIAL)  // 돌발 미션도 공식 미션으로 취급
                .assignedAt(now)
                .dueDate(dueDate)
                .status(UserMissionStatus.ASSIGNED)
                .isSpontaneous(true)  // 돌발 미션으로 표시
                .build();
        
        UserMission saved = userMissionRepository.save(userMission);
        log.info("돌발 미션 할당 완료: userId={}, spontaneousMissionId={}, type={}, title={}, assignedAt={}", 
                user.getId(), spontaneousMission.getId(), missionType, spontaneousMission.getTitle(), now);
        
        return saved;
    }

    /**
     * 사용자에게 일반 미션 할당 (mission 테이블 사용)
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
     * - 감정일기 미션: 감정일기 작성 화면으로 바로 이동
     */
    private void sendSpontaneousMissionNotification(User user, String missionTitle, String missionType, Long userMissionId) {
        try {
            log.info("돌발 미션 알림 전송 시작: userId={}, missionType={}, userMissionId={}, fcmToken={}", 
                    user.getId(), missionType, userMissionId != null ? userMissionId : "null(알림만)", 
                    user.getFcmToken() != null ? "있음" : "없음");
            
            String title;
            String content;
            NotificationType notificationType;
            String referenceType;
            Long referenceId;
            
            // 미션 타입에 따라 알림 내용과 타입 설정
            if ("기상".equals(missionType)) {
                title = "기상 시간입니다! 🌅";
                content = "기상 미션이 도착했습니다. 1일 안에 인증해주세요!";
                notificationType = NotificationType.SPONTANEOUS_WAKE_UP;  // 프론트에서 인증 화면으로 라우팅
                referenceType = "USER_MISSION";
                referenceId = userMissionId;
            } else if (missionType.contains("식사")) {
                title = String.format("%s 시간입니다! 🍽️", missionType);
                content = String.format("%s 미션이 도착했습니다. 게시글을 작성해주세요!", missionType);
                notificationType = NotificationType.SPONTANEOUS_MEAL;  // 프론트에서 인증 화면으로 라우팅
                referenceType = "USER_MISSION";
                referenceId = userMissionId;
            } else if ("감정일기".equals(missionType)) {
                title = "감정일기 작성 시간입니다! ✍️";
                content = "오늘 하루를 돌아보며 감정일기를 작성해보세요.";
                notificationType = NotificationType.SPONTANEOUS_DIARY;  // 프론트에서 감정일기 작성 화면으로 바로 이동
                // 감정일기는 UserMission이 없으므로 참조 정보 없음
                referenceType = null;
                referenceId = null;
            } else {
                title = "돌발 미션이 도착했습니다! 🎯";
                content = String.format("%s 시간입니다. '%s' 미션을 확인해보세요!", missionType, missionTitle);
                notificationType = NotificationType.MISSION_ASSIGNED;
                referenceType = userMissionId != null ? "USER_MISSION" : null;
                referenceId = userMissionId;
            }
            
            Notification savedNotification = notificationService.createAndPushNotification(
                    user,
                    notificationType,
                    title,
                    content,
                    referenceType,
                    referenceId
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
            // 돌발 미션은 mission이 null일 수 있음 (spontaneous_mission 테이블에만 존재)
            if (um.getMission() == null && um.isSpontaneousMission()) {
                // mission이 null인 돌발 미션은 할당 시간을 기준으로 타입을 구분
                String assignedTime = um.getAssignedAt().format(TIME_FORMATTER);
                String userWakeTime = roundTimeTo5Minutes(user.getWakeTime());
                String userSleepTime = roundTimeTo5Minutes(user.getSleepTime());
                
                // 기상 미션 체크: 할당 시간이 사용자 기상 시간과 일치하는지 확인
                if ("기상".equals(missionType)) {
                    if (userWakeTime != null && assignedTime.equals(userWakeTime)) {
                        log.debug("사용자 {}는 오늘 이미 기상 미션(mission=null)이 할당됨: userMissionId={}, assignedAt={}", 
                                user.getId(), um.getId(), um.getAssignedAt());
                        return true;
                    }
                }
                
                // 감정일기 미션 체크: 할당 시간이 사용자 취침 시간과 일치하는지 확인
                if ("감정일기".equals(missionType)) {
                    if (userSleepTime != null && assignedTime.equals(userSleepTime)) {
                        log.debug("사용자 {}는 오늘 이미 감정일기 미션(mission=null)이 할당됨: userMissionId={}, assignedAt={}", 
                                user.getId(), um.getId(), um.getAssignedAt());
                        return true;
                    }
                }
                
                continue;
            }
            
            // mission이 있는 경우 (일반 미션이거나 식사 미션)
            if (um.getMission() == null) {
                continue;
            }
            
            String missionTitle = um.getMission().getTitle();
            
            // 기상 미션 체크
            if ("기상".equals(missionType)) {
                if (missionTitle.contains("기상") || missionTitle.contains("일어나")) {
                    log.debug("사용자 {}는 오늘 이미 기상 미션이 할당됨: missionId={}", user.getId(), um.getMission().getId());
                    return true;
                }
            }
            
            // 식사 미션 체크 (아침/점심/저녁 각각 별도로 체크)
            // 식사 미션은 공통 미션을 사용하므로, 할당 시간과 사용자 설정 시간을 비교하여 구분
            if ("아침".equals(missionType) || "점심".equals(missionType) || "저녁".equals(missionType)) {
                if (missionTitle.contains("식사") || missionTitle.contains("밥")) {
                    // 할당 시간을 정규화하여 사용자 설정 시간과 비교
                    String assignedTime = um.getAssignedAt().format(TIME_FORMATTER);
                    String userMealTime = null;
                    
                    if ("아침".equals(missionType)) {
                        userMealTime = roundTimeTo5Minutes(user.getBreakfastTime());
                    } else if ("점심".equals(missionType)) {
                        userMealTime = roundTimeTo5Minutes(user.getLunchTime());
                    } else if ("저녁".equals(missionType)) {
                        userMealTime = roundTimeTo5Minutes(user.getDinnerTime());
                    }
                    
                    // 같은 시간대의 식사 미션인 경우에만 중복으로 처리
                    if (userMealTime != null && assignedTime.equals(userMealTime)) {
                        log.debug("사용자 {}는 오늘 이미 {} 식사 미션이 할당됨: missionId={}, assignedAt={}", 
                                user.getId(), missionType, um.getMission().getId(), um.getAssignedAt());
                        return true;
                    }
                }
            }
            
            // 감정일기 미션 체크
            if ("감정일기".equals(missionType)) {
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
     * 사용자가 인증을 시도할 때만 1일 초과 여부를 확인하도록 변경
     * 스케줄러에서는 자동으로 실패 처리하지 않음
     */
    private void processExpiredSpontaneousMissions() {
        // 비활성화: 사용자가 인증을 시도할 때만 1일 초과 여부를 확인
        // 스케줄러에서는 자동으로 실패 처리하지 않음
        // log.info("=== 돌발 미션 시간 초과 처리 시작 ===");
        // 기상 미션은 사용자가 인증을 시도할 때만 시간 초과 체크 (UserMissionService.verifyWakeUpMission)
    }
}
