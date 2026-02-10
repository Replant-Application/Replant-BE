package com.app.replant.domain.usermission.entity;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.spontaneousmission.entity.SpontaneousMission;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_mission", indexes = {
        @Index(name = "idx_user_mission_type", columnList = "mission_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 통합된 미션 참조 (공식 미션 + 커스텀 미션 모두)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    // 돌발 미션 참조 (spontaneous_mission 테이블)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spontaneous_mission_id")
    private SpontaneousMission spontaneousMission;

    // 투두리스트 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_list_id")
    private TodoList todoList;

    // 미션 타입 구분 (OFFICIAL / CUSTOM)
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type")
    private MissionType missionType;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserMissionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @Builder
    private UserMission(User user, Mission mission, SpontaneousMission spontaneousMission, 
            TodoList todoList, MissionType missionType,
            LocalDateTime assignedAt, LocalDateTime dueDate, UserMissionStatus status) {
        this.user = user;
        this.mission = mission;
        this.spontaneousMission = spontaneousMission;
        this.todoList = todoList;

        // missionType 결정 로직
        if (missionType != null) {
            this.missionType = missionType;
        } else if (mission != null) {
            // 통합된 Mission 엔티티에서 직접 type 가져오기
            this.missionType = mission.getMissionType();
        }

        this.assignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        this.dueDate = dueDate;
        this.status = status != null ? status : UserMissionStatus.ASSIGNED;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(UserMissionStatus status) {
        this.status = status;
    }

    /**
     * 미션 완료 처리
     */
    public void complete() {
        this.status = UserMissionStatus.COMPLETED;
    }

    /**
     * 미션 실패 처리
     */
    public void fail() {
        this.status = UserMissionStatus.FAILED;
    }

    /**
     * 공식 미션 여부 확인
     */
    public boolean isOfficialMission() {
        if (this.missionType != null) {
            return this.missionType == MissionType.OFFICIAL;
        }
        return this.mission != null && this.mission.isOfficialMission();
    }
    
    /**
     * 돌발 미션 여부 확인 (spontaneousMission이 있는 경우)
     * 돌발 미션은 별도의 SpontaneousMission 엔티티로 관리됩니다.
     */
    public boolean isSpontaneousMission() {
        return this.spontaneousMission != null;
    }

    /**
     * 커스텀 미션 여부 확인
     */
    public boolean isCustomMission() {
        if (this.missionType != null) {
            return this.missionType == MissionType.CUSTOM;
        }
        return this.mission != null && this.mission.isCustomMission();
    }

    /**
     * 미션 ID 조회
     */
    public Long getMissionId() {
        return this.mission != null ? this.mission.getId() : null;
    }

    /**
     * 미션 제목 조회
     */
    public String getMissionTitle() {
        return this.mission != null ? this.mission.getTitle() : "미션";
    }

    /**
     * 미션 타입 반환
     */
    public MissionType getMissionType() {
        if (this.missionType != null) {
            return this.missionType;
        }
        return this.mission != null ? this.mission.getMissionType() : null;
    }

}
