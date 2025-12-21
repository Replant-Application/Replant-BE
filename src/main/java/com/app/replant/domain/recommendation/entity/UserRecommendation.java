package com.app.replant.domain.recommendation.entity;

import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_user_id", nullable = false)
    private User recommendedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_mission_id")
    private CustomMission customMission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_mission_id", nullable = false)
    private UserMission userMission;

    @Column(name = "match_reason", columnDefinition = "json")
    private String matchReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserRecommendation(User user, User recommendedUser, Mission mission, CustomMission customMission,
                              UserMission userMission, String matchReason, LocalDateTime expiresAt) {
        this.user = user;
        this.recommendedUser = recommendedUser;
        this.mission = mission;
        this.customMission = customMission;
        this.userMission = userMission;
        this.matchReason = matchReason;
        this.status = RecommendationStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public void accept() {
        this.status = RecommendationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = RecommendationStatus.REJECTED;
    }

    public enum RecommendationStatus {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }
}
