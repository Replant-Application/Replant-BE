package com.app.replant.scheduler;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.notification.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 미션 자동 배분 스케줄러
 * - 일간: 매일 오전 7시 (KST), 3개 미션
 * - 주간: 매주 월요일 오전 7시 (KST), 3개 미션
 * - 월간: 매월 1일 오전 7시 (KST), 3개 미션
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MissionDistributionScheduler {

    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final NotificationService notificationService;

    private static final int DAILY_MISSION_COUNT = 3;
    private static final int WEEKLY_MISSION_COUNT = 2;
    private static final int MONTHLY_MISSION_COUNT = 1;

    /**
     * 매일 오전 7시 (KST = UTC+9) 일간 미션 배분
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    @Transactional
    public void distributeDailyMissions() {
        log.info("일간 미션 배분 스케줄 시작");
        distributeMissions(MissionType.DAILY, DAILY_MISSION_COUNT, 1);
    }

    /**
     * 매주 월요일 오전 7시 (KST) 주간 미션 배분
     * cron: 초 분 시 일 월 요일(MON=1)
     */
    @Scheduled(cron = "0 0 7 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void distributeWeeklyMissions() {
        log.info("주간 미션 배분 스케줄 시작");
        distributeMissions(MissionType.WEEKLY, WEEKLY_MISSION_COUNT, 7);
    }

    /**
     * 매월 1일 오전 7시 (KST) 월간 미션 배분
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 7 1 * *", zone = "Asia/Seoul")
    @Transactional
    public void distributeMonthlyMissions() {
        log.info("월간 미션 배분 스케줄 시작");
        distributeMissions(MissionType.MONTHLY, MONTHLY_MISSION_COUNT, 30);
    }

    /**
     * 미션 배분 핵심 로직
     */
    private void distributeMissions(MissionType type, int count, int dueDays) {
        // 활성화된 미션 목록 조회
        List<Mission> activeMissions = missionRepository.findActiveByType(type);
        if (activeMissions.isEmpty()) {
            log.warn("{} 타입의 활성화된 미션이 없습니다.", type);
            return;
        }

        // 모든 활성 유저 조회
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("배분할 유저가 없습니다.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusDays(dueDays);

        int totalAssigned = 0;
        int totalNotified = 0;

        for (User user : users) {
            // 랜덤하게 미션 선택
            List<Mission> selectedMissions = selectRandomMissions(activeMissions, count);

            List<UserMission> assignedMissions = new ArrayList<>();
            for (Mission mission : selectedMissions) {
                UserMission userMission = UserMission.builder()
                        .user(user)
                        .mission(mission)
                        .assignedAt(now)
                        .dueDate(dueDate)
                        .status(UserMissionStatus.ASSIGNED)
                        .build();
                assignedMissions.add(userMission);
            }

            userMissionRepository.saveAll(assignedMissions);
            totalAssigned += assignedMissions.size();

            // 알림 생성 + SSE 실시간 푸시
            String typeKorean = getMissionTypeKorean(type);
            String notificationTitle = String.format("새로운 %s 미션이 도착했어요!", typeKorean);
            String notificationContent = String.format("%d개의 %s 미션이 배정되었습니다. 지금 확인해보세요!",
                    selectedMissions.size(), typeKorean);

            notificationService.createAndPushNotification(
                    user,
                    "MISSION_ASSIGNED",
                    notificationTitle,
                    notificationContent,
                    "MISSION_TYPE",
                    null
            );
            totalNotified++;
        }

        log.info("{} 미션 배분 완료 - 배분된 미션: {}, 알림 전송: {}",
                type, totalAssigned, totalNotified);
    }

    /**
     * 미션 목록에서 랜덤하게 선택
     */
    private List<Mission> selectRandomMissions(List<Mission> missions, int count) {
        List<Mission> shuffled = new ArrayList<>(missions);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * 미션 타입 한글 변환
     */
    private String getMissionTypeKorean(MissionType type) {
        return switch (type) {
            case DAILY -> "일간";
            case WEEKLY -> "주간";
            case MONTHLY -> "월간";
        };
    }
}
