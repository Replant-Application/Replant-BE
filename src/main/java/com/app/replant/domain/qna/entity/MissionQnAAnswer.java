package com.app.replant.domain.qna.entity;

import com.app.replant.domain.badge.entity.UserBadge;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mission_qna_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionQnAAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_id", nullable = false)
    private MissionQnA qna;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerer_id", nullable = false)
    private User answerer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private UserBadge badge;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_accepted", nullable = false)
    private Boolean isAccepted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MissionQnAAnswer(MissionQnA qna, User answerer, UserBadge badge, String content, Boolean isAccepted) {
        this.qna = qna;
        this.answerer = answerer;
        this.badge = badge;
        this.content = content;
        this.isAccepted = isAccepted != null ? isAccepted : false;
        this.createdAt = LocalDateTime.now();
    }

    public void accept() {
        this.isAccepted = true;
        this.qna.markAsResolved();
    }
}
