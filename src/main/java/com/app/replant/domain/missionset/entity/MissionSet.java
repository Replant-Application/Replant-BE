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
 * 미션세트 (투두리스트) 엔티티
 * 여러 미션을 묶어서 관리할 수 있는 세트
 */
@Entity
@Table(name = "mission_set", indexes = {
    @Index(name = "idx_mission_set_creator", columnList = "creator_id"),
    @Index(name = "idx_mission_set_is_public", columnList = "is_public"),
    @Index(name = "idx_mission_set_added_count", columnList = "added_count"),
    @Index(name = "idx_mission_set_type", columnList = "set_type")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionSet extends SoftDeletableEntity {

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
    @OneToMany(mappedBy = "missionSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<MissionSetMission> missions = new ArrayList<>();

    // 기존 공유 미션세트용 빌더
    @Builder
    private MissionSet(User creator, String title, String description, Boolean isPublic) {
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
    private static MissionSet createTodoList(User creator, String title, String description, Integer totalCount) {
        MissionSet missionSet = new MissionSet();
        missionSet.creator = creator;
        missionSet.title = title;
        missionSet.description = description;
        missionSet.isPublic = false;
        missionSet.addedCount = 0;
        missionSet.averageRating = 0.0;
        missionSet.reviewCount = 0;
        missionSet.isActive = true;
        missionSet.setType = MissionSetType.TODOLIST;
        missionSet.completedCount = 0;
        missionSet.totalCount = totalCount != null ? totalCount : 5;
        missionSet.todolistStatus = TodoListStatus.ACTIVE;
        missionSet.createdAt = LocalDateTime.now();
        missionSet.updatedAt = LocalDateTime.now();
        return missionSet;
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

    public void addMission(MissionSetMission missionSetMission) {
        this.missions.add(missionSetMission);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeMission(MissionSetMission missionSetMission) {
        this.missions.remove(missionSetMission);
        this.updatedAt = LocalDateTime.now();
    }

    // ============ 투두리스트 관련 메서드들 ============

    /**
     * 투두리스트 미션 완료 처리
     */
    public void incrementCompletedCount() {
        if (this.completedCount == null) {
            this.completedCount = 0;
        }
        this.completedCount++;
        this.updatedAt = LocalDateTime.now();

        // 진행률 확인 후 상태 업데이트
        updateTodoListStatusIfNeeded();
    }

    /**
     * 투두리스트 진행률 계산 (0-100)
     */
    public int getProgressRate() {
        if (this.totalCount == null || this.totalCount == 0) {
            return 0;
        }
        int completed = this.completedCount != null ? this.completedCount : 0;
        return (int) Math.round((double) completed / this.totalCount * 100);
    }

    /**
     * 새 투두리스트 생성 가능 여부 (80% 이상 완료 시)
     */
    public boolean canCreateNewTodoList() {
        return getProgressRate() >= 80;
    }

    /**
     * 투두리스트 상태 업데이트 (80% 이상 완료 시 COMPLETED로 변경)
     */
    private void updateTodoListStatusIfNeeded() {
        if (this.setType == MissionSetType.TODOLIST && canCreateNewTodoList()) {
            this.todolistStatus = TodoListStatus.COMPLETED;
        }
    }

    /**
     * 투두리스트 보관처리
     */
    public void archiveTodoList() {
        if (this.setType == MissionSetType.TODOLIST) {
            this.todolistStatus = TodoListStatus.ARCHIVED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 투두리스트 여부 확인
     */
    public boolean isTodoList() {
        return this.setType == MissionSetType.TODOLIST;
    }
}
