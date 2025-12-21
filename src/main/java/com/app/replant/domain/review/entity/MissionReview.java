package com.app.replant.domain.review.entity;

import com.app.replant.domain.badge.entity.UserBadge;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mission_review", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mission_user", columnNames = {"mission_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private UserBadge badge;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MissionReview(Mission mission, User user, UserBadge badge, String content) {
        this.mission = mission;
        this.user = user;
        this.badge = badge;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
