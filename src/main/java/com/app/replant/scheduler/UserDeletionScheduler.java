package com.app.replant.scheduler;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 삭제 스케줄러
 * 1단계: INACTIVE 상태인 사용자를 30일 후 DELETED로 변경
 * 2단계: DELETED 상태인 사용자를 영구 삭제
 * 
 * 실행 주기: 매일 오전 3시 (KST)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeletionScheduler {

    private final UserRepository userRepository;

    /**
     * 매일 오전 3시에 실행
     * cron: "초 분 시 일 월 요일"
     * zone = "Asia/Seoul"이므로 KST 기준으로 실행
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredUsers() {
        try {
            log.info("=== 사용자 삭제 스케줄러 실행: {} ===", LocalDateTime.now());
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime thirtyDaysAgo = now.minusDays(30);
            
            // 1단계: 30일 지난 INACTIVE 사용자를 DELETED로 변경
            List<User> inactiveUsers = userRepository.findInactiveUsersToMarkAsDeleted(thirtyDaysAgo);
            int inactiveCount = inactiveUsers.size();
            
            if (inactiveCount > 0) {
                log.info("30일 이상 지난 INACTIVE 사용자 수: {}", inactiveCount);
                int markedAsDeleted = userRepository.markInactiveUsersAsDeleted(thirtyDaysAgo);
                log.info("INACTIVE → DELETED 변경 완료: {}/{}", markedAsDeleted, inactiveCount);
            }
            
            // 2단계: DELETED 상태인 사용자를 영구 삭제
            List<User> deletedUsers = userRepository.findUsersToPermanentlyDelete(thirtyDaysAgo);
            int deletedCount = deletedUsers.size();
            
            if (deletedCount > 0) {
                log.info("30일 이상 지난 DELETED 사용자 수: {}", deletedCount);
                int permanentlyDeleted = userRepository.deleteUsersPermanentlyByThreshold(thirtyDaysAgo);
                log.info("영구 삭제 완료: {}/{}", permanentlyDeleted, deletedCount);
            }
            
            if (inactiveCount == 0 && deletedCount == 0) {
                log.info("처리할 사용자가 없습니다.");
            }
            
            log.info("=== 사용자 삭제 스케줄러 완료 ===");
            
        } catch (Exception e) {
            log.error("사용자 삭제 스케줄러 실행 중 예외 발생", e);
            e.printStackTrace();
        }
    }
}
