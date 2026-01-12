package com.app.replant.domain.missionset.entity;

import com.app.replant.common.SoftDeletableEntity;
import com.app.replant.domain.missionset.enums.MissionSetType;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

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
        @Index(name = "idx_todolist_is_public", columnList = "is_public"),
        @Index(name = "idx_todolist_added_count", columnList = "added_count"),
        @Index(name = "idx_todolist_type", columnList = "set_type")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoList extends SoftDeletableEntity {

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

    // 공개 여부
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    // 담은 수 (다른 사용자가 이 미션세트를 담은 횟수)
    @Column(name = "added_count", nullable = false)
    private Integer addedCount;

    // 평균 별점 (리뷰 평균)
    @Column(name = "average_rating", nullable = false)
    private Double averageRating;

    // 리뷰 수
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    // 활성 여부
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============ 투두리스트용 필드들 ============

    // 미션세트 타입: TODOLIST(개인 투두리스트), SHARED(공유 미션세트)
    // TODO: Enum 이름도 TodoListType으로 변경 고려 가능하나 일단 유지
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

    // 기존 공유 미션세트용 빌더
    @Builder
    private TodoList(User creator, String title, String description, Boolean isPublic) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.isPublic = isPublic != null ? isPublic : false;
        this.addedCount = 0;
        this.averageRating = 0.0;
        this.reviewCount = 0;
        this.isActive = true;
        this.setType = MissionSetType.SHARED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 투두리스트 생성용 빌더
    @Builder(builderMethodName = "todoListBuilder")
    private static TodoList createTodoList(User creator, String title, String description, Integer totalCount) {
        TodoList todoList = new TodoList();
        todoList.creator = creator;
        todoList.title = title;
        todoList.description = description;
        todoList.isPublic = false;
        todoList.addedCount = 0;
        todoList.averageRating = 0.0;
        todoList.reviewCount = 0;
        todoList.isActive = true;
        todoList.setType = MissionSetType.TODOLIST;
        todoList.completedCount = 0;
        todoList.totalCount = totalCount != null ? totalCount : 5;
        todoList.todolistStatus = TodoListStatus.ACTIVE;
        todoList.createdAt = LocalDateTime.now();
        todoList.updatedAt = LocalDateTime.now();
        return todoList;
    }

    public void update(String title, String description, Boolean isPublic) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementAddedCount() {
        this.addedCount++;
    }

    public void decrementAddedCount() {
        if (this.addedCount > 0) {
            this.addedCount--;
        }
    }

    public void updateRating(Double newAverageRating, Integer newReviewCount) {
        this.averageRating = newAverageRating;
        this.reviewCount = newReviewCount;
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCreator(Long userId) {
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
