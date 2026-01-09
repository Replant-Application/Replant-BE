package com.app.replant.domain.missionset.entity;

import com.app.replant.domain.mission.entity.Mission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 미션세트-미션 연결 엔티티
 * 미션세트에 포함된 미션들을 관리
 */
@Entity
@Table(name = "mission_set_mission", indexes = {
    @Index(name = "idx_msm_mission_set", columnList = "mission_set_id"),
    @Index(name = "idx_msm_mission", columnList = "mission_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_mission_set_mission", columnNames = {"mission_set_id", "mission_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionSetMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_set_id", nullable = false)
    private MissionSet missionSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    // 표시 순서
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MissionSetMission(MissionSet missionSet, Mission mission, Integer displayOrder) {
        this.missionSet = missionSet;
        this.mission = mission;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.createdAt = LocalDateTime.now();
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
