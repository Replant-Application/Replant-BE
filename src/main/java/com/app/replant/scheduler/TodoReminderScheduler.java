package com.app.replant.scheduler;

import com.app.replant.domain.missionset.repository.TodoListRepository;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 투두리스트 생성 알림 스케줄러
 * 매일 오전 7시(KST)에 기존 가입자(투두리스트가 1개 이상인 사용자)에게 투두리스트 생성 알림 발송
 * 신규 가입자(투두리스트 0개)는 제외
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TodoReminderScheduler {

    private final UserRepository userRepository;
    private final TodoListRepository todoListRepository;
    private final NotificationService notificationService;

    /**
     * 매일 오전 7시(KST) 실행
     * cron: "초 분 시 일 월 요일"
     * zone = "Asia/Seoul"이므로 KST 기준으로 실행
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void sendDailyTodoReminder() {
        log.info("=== 투두리스트 생성 알림 스케줄러 시작 (오전 7시) ===");

        try {
            // 모든 활성 사용자 조회
            List<User> activeUsers = userRepository.findAllActiveUsers();
            log.info("전체 활성 사용자 수: {}", activeUsers.size());

            // 기존 가입자만 필터링 (투두리스트가 1개 이상인 사용자)
            List<User> existingUsers = activeUsers.stream()
                    .filter(user -> {
                        long todoListCount = todoListRepository.countAllTodoListsByCreator(user);
                        return todoListCount > 0; // 투두리스트가 1개 이상인 경우만
                    })
                    .collect(Collectors.toList());

            log.info("기존 가입자 수 (투두리스트 1개 이상): {}", existingUsers.size());

            int successCount = 0;
            int failCount = 0;

            for (User user : existingUsers) {
                try {
                    // 알림 생성 및 발송
                    notificationService.createAndPushNotification(
                            user,
                            NotificationType.SYSTEM,
                            "투두리스트 작성 알림",
                            "오늘의 투두리스트를 작성해보세요!");
                    successCount++;
                } catch (Exception e) {
                    log.error("사용자 {}에게 알림 발송 실패", user.getId(), e);
                    failCount++;
                }
            }

            log.info("=== 투두리스트 생성 알림 스케줄러 완료 === 성공: {}, 실패: {}", successCount, failCount);
        } catch (Exception e) {
            log.error("투두리스트 알림 스케줄러 실행 중 오류 발생", e);
        }
    }
}
