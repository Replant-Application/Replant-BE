package com.app.replant.domain.qna.entity;

import com.app.replant.common.BaseEntity;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mission_qna")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionQnA extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questioner_id", nullable = false)
    private User questioner;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;

    @OneToMany(mappedBy = "qna", fetch = FetchType.LAZY)
    private List<MissionQnAAnswer> answers = new ArrayList<>();

    @Builder
    private MissionQnA(Mission mission, User questioner, String question, Boolean isResolved) {
        this.mission = mission;
        this.questioner = questioner;
        this.question = question;
        this.isResolved = isResolved != null ? isResolved : false;
    }

    public void resolve() {
        this.isResolved = true;
    }

    public void markAsResolved() {
        this.isResolved = true;
    }

    public boolean isQuestioner(Long userId) {
        return this.questioner.getId().equals(userId);
    }
}
