package com.app.replant.domain.missionset.entity;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.missionset.enums.MissionSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 투두리스트-미션 연결 엔티티
 * (구 MissionSetMission)
 */
@Entity
@Table(name = "todolist_mission", indexes = {
        @Index(name = "idx_todolist_mission_todolist", columnList = "todolist_id"),
        @Index(name = "idx_todolist_mission_mission", columnList = "mission_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_todolist_mission", columnNames = { "todolist_id", "mission_id" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoListMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todolist_id", nullable = false)
    private TodoList todoList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    // 표시 순서
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ============ 투두리스트용 필드들 ============

    // 미션 완료 여부 (투두리스트용)
    @Column(name = "is_completed")
    private Boolean isCompleted;

    // 미션 완료 시간 (투두리스트용)
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 미션 출처: RANDOM_OFFICIAL(랜덤 배정), CUSTOM_SELECTED(사용자 선택)
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_source", length = 20)
    private MissionSource missionSource;

    // 기존 공유 미션세트용 빌더
    @Builder
    private TodoListMission(TodoList todoList, Mission mission, Integer displayOrder) {
        this.todoList = todoList;
        this.mission = mission;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.createdAt = LocalDateTime.now();
    }

    // 투두리스트 미션용 빌더
    @Builder(builderMethodName = "todoMissionBuilder")
    private static TodoListMission createTodoMission(
            TodoList todoList,
            Mission mission,
            Integer displayOrder,
            MissionSource missionSource) {
        TodoListMission msm = new TodoListMission();
        msm.todoList = todoList;
        msm.mission = mission;
        msm.displayOrder = displayOrder != null ? displayOrder : 0;
        msm.isCompleted = false;
        msm.missionSource = missionSource;
        msm.createdAt = LocalDateTime.now();
        return msm;
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * 미션 완료 처리
     */
    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 미션 완료 여부 확인
     */
    public boolean isCompletedMission() {
        return Boolean.TRUE.equals(this.isCompleted);
    }
}
