package com.app.replant.domain.missionset.entity;

import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Index(name = "idx_mission_set_added_count", columnList = "added_count")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionSet {

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

    // 미션세트에 포함된 미션 목록
    @OneToMany(mappedBy = "missionSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<MissionSetMission> missions = new ArrayList<>();

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
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
}
