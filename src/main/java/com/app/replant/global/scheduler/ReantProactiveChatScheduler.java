package com.app.replant.global.scheduler;

import com.app.replant.domain.chat.repository.ChatLogRepository;
import com.app.replant.domain.chat.service.ChatService;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 리앤트 선제 메시지 스케줄러
 * 조건에 따라 리앤트가 먼저 사용자에게 메시지를 보냅니다.
 *
 * 실행 주기: 매 2시간마다
 * 제한: 사용자당 하루 최대 1회
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReantProactiveChatScheduler {

    private final ChatLogRepository chatLogRepository;
    private final ChatService chatService;
    private final ReantRepository reantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 하루 최대 선제 메시지 수
    private static final int MAX_PROACTIVE_PER_DAY = 1;

    // 비활성 기준 (시간)
    private static final int INACTIVITY_HOURS = 24;

    /** S2245: 보안·암호학적 용도가 아니어도 SonarCloud PRNG 경고 회피용 */
    private static final SecureRandom secureRandom = new SecureRandom();

    // ============================================
    // 메시지 템플릿
    // ============================================

    private static final String[] INACTIVITY_MESSAGES = {
            "오랜만이야~ 나 보고 싶지 않았어? 😊",
            "요즘 바빠? 나 심심해~ 놀아줘! 🐾",
            "오랜만에 연락해봐! 얘기하고 싶어 💬",
            "보고 싶었어~ 오늘은 어떤 하루였어? 🌟",
    };

    private static final String[] HUNGRY_MESSAGES = {
            "배고파... 밥 좀 줘~ 🍚",
            "꼬르륵... 배에서 소리가 나! 😢",
            "맛있는 거 먹고 싶어... 🤤",
    };

    private static final String[] LOW_MOOD_MESSAGES = {
            "오늘 기분이 좀 안 좋아... 얘기 좀 할래? 💙",
            "심심하고 우울해... 놀아줄래? 🥺",
            "기운이 없어... 같이 얘기하자 💭",
    };

    /**
     * 매 2시간마다 실행 (08시~22시)
     */
    @Scheduled(cron = "0 0 8,10,12,14,16,18,20,22 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendProactiveMessages() {
        log.info("=== 리앤트 선제 메시지 스케줄러 시작 ===");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime inactivityThreshold = now.minusHours(INACTIVITY_HOURS);

            // 채팅 기록이 있는 사용자 목록 조회
            List<Long> userIds = chatLogRepository.findDistinctUserIds();
            int sentCount = 0;

            for (Long userId : userIds) {
                try {
                    // 오늘 이미 선제 메시지를 보냈으면 스킵
                    Long proactiveToday = chatLogRepository.countProactiveTodayByUserId(userId, todayStart);
                    if (proactiveToday >= MAX_PROACTIVE_PER_DAY) {
                        continue;
                    }

                    // 리앤트 조회
                    Optional<Reant> reantOpt = reantRepository.findByUserId(userId);
                    if (reantOpt.isEmpty()) {
                        continue;
                    }
                    Reant reant = reantOpt.get();

                    // 메시지 선택 (우선순위: 배고픔 > 기분 > 비활성)
                    String message = selectMessage(userId, reant, inactivityThreshold);
                    if (message == null) {
                        continue;
                    }

                    // 사용자 조회
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isEmpty()) {
                        continue;
                    }
                    User user = userOpt.get();

                    // 선제 메시지 저장
                    chatService.createProactiveMessage(user, reant, message);

                    // 푸시 알림 전송
                    notificationService.createAndPushNotification(
                            user,
                            NotificationType.CHAT_MESSAGE,
                            reant.getName(),
                            message
                    );

                    sentCount++;
                    log.info("[선제메시지] 전송 완료 - userId: {}, reant: {}", userId, reant.getName());

                } catch (Exception e) {
                    log.error("[선제메시지] userId: {} 처리 중 오류: {}", userId, e.getMessage());
                }
            }

            log.info("=== 리앤트 선제 메시지 스케줄러 완료 === 전송: {}건", sentCount);

        } catch (Exception e) {
            log.error("리앤트 선제 메시지 스케줄러 실행 중 오류", e);
        }
    }

    /**
     * 조건에 따라 적절한 메시지 선택
     * @return 보낼 메시지, 조건에 해당하지 않으면 null
     */
    private String selectMessage(Long userId, Reant reant, LocalDateTime inactivityThreshold) {
        // 1. 배고픔이 높으면 (hunger > 70)
        if (reant.getHunger() > 70) {
            return pickRandom(HUNGRY_MESSAGES);
        }

        // 2. 기분이 낮으면 (mood < 50)
        if (reant.getMood() < 50) {
            return pickRandom(LOW_MOOD_MESSAGES);
        }

        // 3. 마지막 채팅이 24시간 이상 전이면
        LocalDateTime lastChat = chatLogRepository.findLastChatTimeByUserId(userId);
        if (lastChat != null && lastChat.isBefore(inactivityThreshold)) {
            return pickRandom(INACTIVITY_MESSAGES);
        }

        return null;
    }

    private String pickRandom(String[] messages) {
        return messages[secureRandom.nextInt(messages.length)];
    }
}
