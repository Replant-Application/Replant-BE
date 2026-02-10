package com.app.replant.domain.spontaneousmission.entity;

import com.app.replant.domain.meallog.entity.MealLog;
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.spontaneousmission.enums.SpontaneousMissionType;
import com.app.replant.domain.user.entity.User;
import com.app.replant.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "spontaneous_mission", indexes = {
        @Index(name = "idx_spontaneous_user_date", columnList = "user_id, assigned_at"),
        @Index(name = "idx_spontaneous_status", columnList = "status"),
        @Index(name = "idx_spontaneous_type", columnList = "mission_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpontaneousMission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false, length = 20)
    private SpontaneousMissionType missionType;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "exp_reward", nullable = false)
    private Integer expReward = 10;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deadline_minutes")
    private Integer deadlineMinutes;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ASSIGNED";

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_log_id")
    private MealLog mealLog;

    @Builder
    public SpontaneousMission(User user, SpontaneousMissionType missionType, String title, String description,
                              Integer expReward, Boolean isActive, Integer deadlineMinutes,
                              LocalDateTime assignedAt, LocalDateTime deadlineAt, String status,
                              LocalDateTime verifiedAt, Post post, MealLog mealLog) {
        this.user = user;
        this.missionType = missionType;
        this.title = title;
        this.description = description;
        this.expReward = expReward != null ? expReward : 10;
        this.isActive = isActive != null ? isActive : true;
        this.deadlineMinutes = deadlineMinutes;
        this.assignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        this.deadlineAt = deadlineAt;
        this.status = status != null ? status : "ASSIGNED";
        this.verifiedAt = verifiedAt;
        this.post = post;
        this.mealLog = mealLog;
    }
}
