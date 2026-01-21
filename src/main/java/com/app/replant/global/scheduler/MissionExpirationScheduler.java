package com.app.replant.global.scheduler;

import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.repository.TodoListRepository;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미션 만료 처리 스케줄러
 * 매시간 실행하여 기한이 지난 미션을 자동으로 실패 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MissionExpirationScheduler {

    private final UserMissionRepository userMissionRepository;
    private final TodoListRepository todoListRepository;

    /**
     * 매시간 정각에 실행
     * 기한이 지난 미션을 FAILED 상태로 변경
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processExpiredMissions() {
        log.info("=== 미션 만료 처리 스케줄러 시작 ===");

        try {
            LocalDateTime now = LocalDateTime.now();

            // 1. 기한이 지난 ASSIGNED 상태의 UserMission 조회
            List<UserMission> expiredMissions = userMissionRepository.findExpiredMissions(now);
            log.info("만료된 미션 수: {}", expiredMissions.size());

            int failedCount = 0;
            for (UserMission mission : expiredMissions) {
                try {
                    mission.fail();
                    failedCount++;
                    log.debug("미션 만료 처리: missionId={}, userId={}",
                            mission.getMission().getId(), mission.getUser().getId());
                } catch (Exception e) {
                    log.error("미션 만료 처리 실패: userMissionId={}", mission.getId(), e);
                }
            }

            // 2. 활성 투두리스트 중 모든 미션이 완료된 경우 투두리스트도 완료 처리
            List<TodoList> activeTodoLists = todoListRepository.findAllActiveTodoLists();
            log.info("활성 투두리스트 수: {}", activeTodoLists.size());

            int completedTodoLists = 0;
            for (TodoList todoList : activeTodoLists) {
                try {
                    // 완료된 미션 수가 총 미션 수와 같으면 자동 완료
                    Integer totalCount = todoList.getTotalCount();
                    Integer completedCount = todoList.getCompletedCount();

                    if (totalCount != null && completedCount != null && completedCount >= totalCount) {
                        todoList.completeTodoList();
                        completedTodoLists++;
                        log.info("투두리스트 자동 완료: todoListId={}, completedCount={}/{}",
                                todoList.getId(), completedCount, totalCount);
                    }
                } catch (Exception e) {
                    log.error("투두리스트 상태 업데이트 실패: todoListId={}", todoList.getId(), e);
                }
            }

            log.info("=== 미션 만료 처리 스케줄러 완료 === 미션 실패: {}, 투두리스트 완료: {}",
                    failedCount, completedTodoLists);
        } catch (Exception e) {
            log.error("미션 만료 처리 스케줄러 실행 중 오류 발생", e);
        }
    }
}
