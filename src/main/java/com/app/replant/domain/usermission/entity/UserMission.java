package com.app.replant.domain.usermission.entity;

import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_mission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_mission_id")
    private CustomMission customMission;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserMissionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private UserMission(User user, Mission mission, CustomMission customMission, LocalDateTime assignedAt, LocalDateTime dueDate, UserMissionStatus status) {
        this.user = user;
        this.mission = mission;
        this.customMission = customMission;
        this.assignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        this.dueDate = dueDate;
        this.status = status != null ? status : UserMissionStatus.ASSIGNED;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(UserMissionStatus status) {
        this.status = status;
    }
}
