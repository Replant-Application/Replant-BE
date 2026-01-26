package com.app.replant.domain.meallog.entity;

import com.app.replant.domain.meallog.enums.MealLogStatus;
import com.app.replant.domain.meallog.enums.MealType;
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.user.entity.User;
import com.app.replant.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_log", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_meal_date", columnNames = {"user_id", "meal_type", "meal_date"})
    },
    indexes = {
        @Index(name = "idx_meal_log_user_date", columnList = "user_id, meal_date"),
        @Index(name = "idx_meal_log_status", columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Integer rating;  // 맛 평점 1-5

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;  // 연결된 게시글 (인증용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MealLogStatus status;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;  // 인증 마감 시간 (할당 후 2시간 등)

    @Column(name = "exp_reward")
    private Integer expReward;

    @Builder
    public MealLog(User user, MealType mealType, LocalDate mealDate, String title, 
                   String description, Integer rating, Post post, MealLogStatus status,
                   LocalDateTime assignedAt, LocalDateTime deadlineAt, Integer expReward) {
        this.user = user;
        this.mealType = mealType;
        this.mealDate = mealDate;
        this.title = title;
        this.description = description;
        this.rating = rating;
        this.post = post;
        this.status = status;
        this.assignedAt = assignedAt;
        this.deadlineAt = deadlineAt;
        this.expReward = expReward != null ? expReward : 15;  // 기본 경험치
    }

    /**
     * 미션 할당 (스케줄러에서 호출)
     */
    public static MealLog assign(User user, MealType mealType, LocalDate mealDate, int deadlineMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return MealLog.builder()
                .user(user)
                .mealType(mealType)
                .mealDate(mealDate)
                .status(MealLogStatus.ASSIGNED)
                .assignedAt(now)
                .deadlineAt(now.plusMinutes(deadlineMinutes))
                .expReward(15)
                .build();
    }

    /**
     * 인증 완료
     */
    public void verify(Post post, String title, String description, Integer rating) {
        this.post = post;
        this.title = title;
        this.description = description;
        this.rating = rating;
        this.status = MealLogStatus.COMPLETED;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * 실패 처리 (시간 초과)
     */
    public void fail() {
        this.status = MealLogStatus.FAILED;
    }

    /**
     * 건너뛰기
     */
    public void skip() {
        this.status = MealLogStatus.SKIPPED;
    }

    /**
     * 인증 가능 여부 확인 (마감 시간 전 & ASSIGNED 상태)
     */
    public boolean canVerify() {
        return this.status == MealLogStatus.ASSIGNED 
                && (this.deadlineAt == null || LocalDateTime.now().isBefore(this.deadlineAt));
    }

    /**
     * 시간 초과 여부 확인
     */
    public boolean isExpired() {
        return this.deadlineAt != null && LocalDateTime.now().isAfter(this.deadlineAt);
    }

    /**
     * 소유자 확인
     */
    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }

    /**
     * 남은 시간 (초) 계산
     */
    public long getRemainingSeconds() {
        if (deadlineAt == null) {
            return Long.MAX_VALUE;
        }
        long remaining = java.time.Duration.between(LocalDateTime.now(), deadlineAt).getSeconds();
        return Math.max(0, remaining);
    }
}
