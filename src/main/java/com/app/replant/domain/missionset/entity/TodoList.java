package com.app.replant.domain.missionset.entity;

import com.app.replant.domain.missionset.enums.MissionSetType;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.missionset.entity.TodoListMission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 투두리스트 엔티티
 * (구 MissionSet)
 */
@Entity
@Table(name = "todolist", indexes = {
        @Index(name = "idx_todolist_creator", columnList = "creator_id"),
        @Index(name = "idx_todolist_type", columnList = "set_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 활성 여부 (개인 투두리스트 활성화 여부)
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    // 공개 여부 (커뮤니티 공유 게시판에 표시되는지)
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============ 투두리스트용 필드들 ============

    // 미션세트 타입: TODOLIST(개인 투두리스트)만 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "set_type", length = 20)
    private MissionSetType setType;

    // 완료된 미션 수 (투두리스트용)
    @Column(name = "completed_count")
    private Integer completedCount;

    // 총 미션 수 (투두리스트용)
    @Column(name = "total_count")
    private Integer totalCount;

    // 투두리스트 상태: ACTIVE, COMPLETED, ARCHIVED
    @Enumerated(EnumType.STRING)
    @Column(name = "todolist_status", length = 20)
    private TodoListStatus todolistStatus;

    // 미션세트에 포함된 미션 목록
    @OneToMany(mappedBy = "todoList", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<TodoListMission> missions = new ArrayList<>();

    // 투두리스트 생성용 빌더 (유일한 생성 방법)
    @Builder(builderMethodName = "todoListBuilder")
    private static TodoList createTodoList(User creator, String title, String description, Integer totalCount) {
        TodoList todoList = new TodoList();
        todoList.creator = creator;
        todoList.title = title;
        todoList.description = description;
        todoList.isActive = true; // 투두리스트는 기본적으로 활성화 (개인 투두리스트는 항상 활성)
        todoList.isPublic = false; // 기본적으로 비공개, 사용자가 공유 버튼을 눌렀을 때만 공개
        todoList.setType = MissionSetType.TODOLIST;
        todoList.completedCount = 0;
        todoList.totalCount = totalCount != null ? totalCount : 5;
        todoList.todolistStatus = TodoListStatus.ACTIVE;
        todoList.createdAt = LocalDateTime.now();
        todoList.updatedAt = LocalDateTime.now();
        return todoList;
    }

    public void update(String title, String description) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCreator(Long userId) {
        if (this.creator == null || userId == null) {
            return false;
        }
        return this.creator.getId().equals(userId);
    }

    public void addMission(TodoListMission todoListMission) {
        this.missions.add(todoListMission);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeMission(TodoListMission todoListMission) {
        this.missions.remove(todoListMission);
        this.updatedAt = LocalDateTime.now();
    }

    // ============ 투두리스트 관련 메서드들 ============

    public void incrementCompletedCount() {
        if (this.completedCount == null) {
            this.completedCount = 0;
        }
        this.completedCount++;
        this.updatedAt = LocalDateTime.now();
        updateTodoListStatusIfNeeded();
    }

    public int getProgressRate() {
        if (this.totalCount == null || this.totalCount == 0) {
            return 0;
        }
        int completed = this.completedCount != null ? this.completedCount : 0;
        return (int) Math.round((double) completed / this.totalCount * 100);
    }

    public boolean canCreateNewTodoList() {
        return getProgressRate() >= 80;
    }

    private void updateTodoListStatusIfNeeded() {
        if (this.setType == MissionSetType.TODOLIST && canCreateNewTodoList()) {
            this.todolistStatus = TodoListStatus.COMPLETED;
        }
    }

    public void archiveTodoList() {
        if (this.setType == MissionSetType.TODOLIST) {
            this.todolistStatus = TodoListStatus.ARCHIVED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void completeTodoList() {
        if (this.setType == MissionSetType.TODOLIST) {
            this.todolistStatus = TodoListStatus.COMPLETED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean isTodoList() {
        return this.setType == MissionSetType.TODOLIST;
    }
}
