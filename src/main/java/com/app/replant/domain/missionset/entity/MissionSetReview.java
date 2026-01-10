package com.app.replant.domain.missionset.entity;

import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 미션세트 리뷰 엔티티
 * 사용자가 담은 미션세트에 대한 별점 및 리뷰
 */
@Entity
@Table(name = "mission_set_review",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_mission_set_user", columnNames = {"mission_set_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_mission_set_review_set", columnList = "mission_set_id"),
        @Index(name = "idx_mission_set_review_user", columnList = "user_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionSetReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_set_id", nullable = false)
    private MissionSet missionSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating; // 1-5 별점

    @Column(columnDefinition = "TEXT")
    private String content; // 리뷰 내용 (선택)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private MissionSetReview(MissionSet missionSet, User user, Integer rating, String content) {
        this.missionSet = missionSet;
        this.user = user;
        this.rating = rating != null ? Math.min(5, Math.max(1, rating)) : 5;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(Integer rating, String content) {
        if (rating != null) {
            this.rating = Math.min(5, Math.max(1, rating));
        }
        if (content != null) {
            this.content = content;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
