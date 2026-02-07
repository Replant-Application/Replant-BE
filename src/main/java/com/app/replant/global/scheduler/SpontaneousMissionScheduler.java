package com.app.replant.global.scheduler;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.repository.MissionRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 돌발 미션 스케줄러
 * 사용자가 설정한 기상 시간에 맞춰 기상 미션을 자동 할당합니다.
 * 실행 주기: 매 1분마다
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpontaneousMissionScheduler {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FcmService fcmService;
    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

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
            log.info("=== 스케줄러 실행 확인 (KST): {} ===", LocalDateTime.now(ZONE_SEOUL));
            assignSpontaneousMissionsInternal();
        } catch (Exception e) {
            log.error("돌발 미션 할당 스케줄러 실행 중 예외 발생", e);
            e.printStackTrace();
        }
        
        // TODO: SpontaneousMissionService가 삭제되어 만료 처리 기능 비활성화
        // 필요시 UserMissionService에 통합 또는 별도 구현 필요
    }

    /**
     * 돌발 미션 할당
     */
    private void assignSpontaneousMissionsInternal() {
        log.info("=== 돌발 미션 할당 스케줄러 시작 ===");
        
        try {
            // 사용자 기상 시간은 한국 시간으로 설정되므로, 매칭도 KST 기준으로 수행 (서버 타임존과 무관)
            LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);
            LocalTime currentTime = now.toLocalTime();
            
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

    /** 기상 미션 제목 (Mission 테이블 시드/V35와 일치) */
    private static final String WAKE_UP_MISSION_TITLE = "기상 미션";

    /**
     * 기상 미션 할당
     * - Mission "기상 미션"으로 UserMission 생성 후 알림 전송 (앱에서 인증 시 userMissionId 사용)
     */
    private void assignWakeUpMission(User user, LocalDateTime now) {
        Mission wakeMission = missionRepository.findByTitleAndMissionTypeAndIsActive(WAKE_UP_MISSION_TITLE, MissionType.OFFICIAL)
                .orElse(null);
        if (wakeMission == null) {
            log.warn("기상 미션(Mission)을 찾을 수 없습니다. title={}", WAKE_UP_MISSION_TITLE);
            return;
        }

        LocalDate today = now.toLocalDate();
        List<UserMission> todayMissions = userMissionRepository.findByUserIdAndAssignedDate(user.getId(), today);
        boolean alreadyAssigned = todayMissions.stream()
                .anyMatch(um -> wakeMission.getId().equals(um.getMissionId())
                        && (um.getStatus() == UserMissionStatus.ASSIGNED || um.getStatus() == UserMissionStatus.PENDING));
        if (alreadyAssigned) {
            log.debug("기상 미션 이미 오늘 할당됨: userId={}", user.getId());
            return;
        }

        LocalDateTime dueDate = now.plusMinutes(10);
        UserMission userMission = UserMission.builder()
                .user(user)
                .mission(wakeMission)
                .missionType(MissionType.OFFICIAL)
                .assignedAt(now)
                .dueDate(dueDate)
                .status(UserMissionStatus.ASSIGNED)
                .build();
        UserMission saved = userMissionRepository.save(userMission);

        log.info("기상 미션 할당 완료: userId={}, userMissionId={}, dueAt={}", user.getId(), saved.getId(), dueDate);
        sendSpontaneousMissionNotification(user, "기상하기", "기상", saved.getId());
    }

    /**
     * 돌발 미션 알림 전송 (기상 미션만)
     */
    private void sendSpontaneousMissionNotification(User user, String missionTitle, String missionType, Long missionId) {
        // TODO: missionId가 null일 수 있음 (SpontaneousMissionService 삭제로 인해)
        // 임시로 null 허용하되 로그 남김
        if (missionId == null) {
            log.warn("missionId가 null입니다. 알림은 전송하지만 참조 ID는 없습니다. userId={}, missionType={}", user.getId(), missionType);
        }
        
        try {
            log.info("돌발 미션 알림 전송 시작: userId={}, missionType={}, missionId={}, fcmToken={}", 
                    user.getId(), missionType, missionId, user.getFcmToken() != null ? "있음" : "없음");
            
            String title;
            String content;
            NotificationType notificationType;
            
            // 미션 타입에 따라 알림 내용과 타입 설정
            title = "기상 시간입니다! 🌅";
            content = "기상 미션이 도착했습니다. 10분 안에 인증해주세요!";
            notificationType = NotificationType.SPONTANEOUS_WAKE_UP;
            
            String referenceType = (missionId != null) ? "USER_MISSION" : "SPONTANEOUS_MISSION";
            Notification savedNotification = notificationService.createAndPushNotification(
                    user,
                    notificationType,
                    title,
                    content,
                    referenceType,
                    missionId
            );
            
            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
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
